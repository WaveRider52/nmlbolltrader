/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2019 Ta4j Organization & respective
 * authors (see AUTHORS)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.oodabas;

import ch.oodabas.loaders.CsvTradesLoader;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

/**
 * Test your strategy
 * <p>
 * Global example.
 */
public class Quickstart {

    public static void main(final String[] args) {

        // Getting a bar series (from any provider: CSV, web service, etc.)
        final BarSeries series = CsvTradesLoader.loadBitstampSeries();

        // Or within an indicator:
        final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // Getting the simple moving average (SMA) of the close price over the last 20
        // bars
        final SMAIndicator sma = new SMAIndicator(closePrice, 20);

        final StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, 20);
        final BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        final BollingerBandsLowerIndicator nmlUP = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(-1));

        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        // - if the price goes above NML upper band
        final Rule buyingRule = new CrossedUpIndicatorRule(closePrice, nmlUP);

        // Selling rules
        // We want to sell:
        // - if the price goes below NML upper band
        final Rule sellingRule = new CrossedDownIndicatorRule(closePrice, nmlUP);

        // Running our juicy trading strategy...
        final BarSeriesManager seriesManager = new BarSeriesManager(series);
        final TradingRecord tradingRecord = seriesManager.run(new BaseStrategy(buyingRule, sellingRule));
        System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());

        tradingRecord.getTrades().forEach(System.out::println);
        // Analysis

        // Getting the profitable trades ratio
        final AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
        System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));

        // Getting the reward-risk ratio
        final AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
        System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

        // Total profit of our strategy
        // vs total profit of a buy-and-hold strategy
        final AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
        System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));

        // Your turn!
    }
}
