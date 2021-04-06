package ch.oodabas.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;

public class RedCandleIndicator extends CachedIndicator<Boolean> {

    private static final long serialVersionUID = 1096705357145393144L;

    RedCandleIndicator(final BarSeries series) {
        super(series);
    }

    @Override
    protected Boolean calculate(final int index) {
        if (index < 1) {
            return false;
        } else {
            final Bar currBar = this.getBarSeries().getBar(index);
            return currBar.isBearish();
        }
    }

}
