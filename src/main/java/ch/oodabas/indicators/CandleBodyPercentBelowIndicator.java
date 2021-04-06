package ch.oodabas.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class CandleBodyPercentBelowIndicator extends CachedIndicator<Num> {
    private static final long serialVersionUID = -470998235220856042L;
    private final Indicator<Num> indicator;

    CandleBodyPercentBelowIndicator(final Indicator<Num> indicator) {
        super(indicator);
        this.indicator = indicator;
    }

    @Override
    protected Num calculate(final int index) {
        final Bar bar = this.getBarSeries().getBar(index);
        final Num closePrice = bar.getClosePrice();
        final Num openPrice = bar.getOpenPrice();
        final Num indicatorValue = this.indicator.getValue(index);
        final boolean openIsBelow = openPrice.isLessThan(indicatorValue);
        final boolean closeIsBelow = closePrice.isLessThan(indicatorValue);
        if (openIsBelow && closeIsBelow) {
            return numOf(1);
        } else if (openIsBelow && !closeIsBelow) {
            return indicatorValue.minus(openPrice).dividedBy(closePrice.minus(openPrice));
        } else if (closeIsBelow && !openIsBelow) {
            return indicatorValue.minus(closePrice).dividedBy(openPrice.minus(closePrice));
        }
        return numOf(0);
    }
}
