package ch.oodabas.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;

class BollingerNoMansLand75Below {
    static Strategy get(final BarSeries series) {
        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator sma = new SMAIndicator(closePriceIndicator, 20);

        final StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePriceIndicator, 20);
        final BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        final BollingerBandsLowerIndicator nmlUP = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(-1.0));


        final EMAIndicator shortEma = new EMAIndicator(closePriceIndicator, 9);
        final EMAIndicator longEma = new EMAIndicator(closePriceIndicator, 26);
        final MACDIndicator macd = new MACDIndicator(closePriceIndicator, 9, 26);
        final EMAIndicator emaMacd = new EMAIndicator(macd, 18);
        final StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

        // Entry rule
        final Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new OverIndicatorRule(macd, emaMacd))
                .and(new CrossedUpIndicatorRule(closePriceIndicator, nmlUP)); // Signal 2


        // Exit rule
        final Rule exitRule = new BooleanIndicatorRule(new RedCandleIndicator(series))
                .and(new OverIndicatorRule(new CandleBodyPercentBelowIndicator(nmlUP), 0.75)); // Trend


        final Strategy baseStrategy = new BaseStrategy(entryRule, exitRule);
        baseStrategy.setUnstablePeriod(27);

        return baseStrategy;
    }
}

