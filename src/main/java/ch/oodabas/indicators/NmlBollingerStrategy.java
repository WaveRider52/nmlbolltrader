package ch.oodabas.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;

class NmlBollingerStrategy {
    static Strategy get(final BarSeries series) {
        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator sma = new SMAIndicator(closePriceIndicator, 20);

        final StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePriceIndicator, 20);
        final BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        final BollingerBandsLowerIndicator nmlUP = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(-1));

        final CrossedUpIndicatorRule buyRule = new CrossedUpIndicatorRule(closePriceIndicator, nmlUP);
        final OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);
        final CrossedUpIndicatorRule crossUpBuyRule
                = new CrossedUpIndicatorRule(new DoublePercentBIndicator(closePriceIndicator, openPriceIndicator, series), 0.5);

        return new BaseStrategy(buyRule, crossUpBuyRule.and(new BooleanIndicatorRule(new RedCandleIndicator(series))));

    }
}
