package ch.oodabas.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

public class NmlBollingerStrategy {
    public static Strategy get(BarSeries series){
        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator sma = new SMAIndicator(closePriceIndicator, 20);

        final StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePriceIndicator, 20);
        final BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        final BollingerBandsLowerIndicator nmlUP = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(-1));

        CrossedUpIndicatorRule buyRule = new CrossedUpIndicatorRule(closePriceIndicator, nmlUP);
        OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
        CrossedUpIndicatorRule crossUpBuyRule = new CrossedUpIndicatorRule(new DoublePercentBIndicator(closePriceIndicator, openPriceIndicator, series), 0.5);

        return new BaseStrategy(buyRule, crossUpBuyRule.and(RED_RULE));
    }
}
