package ch.oodabas.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.bollinger.PercentBIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;

public class DoublePercentBIndicator extends CachedIndicator<Num> {


    private final PercentBIndicator pcbOne;
    private final PercentBIndicator pcbTwo;

    public DoublePercentBIndicator(Indicator<Num> indicatorOne, Indicator<Num> indicatorTwo, BarSeries series) {
        super(series);
        pcbOne = new PercentBIndicator(indicatorOne, 20, -1);
        pcbTwo = new PercentBIndicator(indicatorTwo, 20, -1);
    }

    @Override
    protected Num calculate(int index) {
        Num valueOne = pcbOne.getValue(index);
        Num valueTwo = pcbTwo.getValue(index);
        return valueOne.plus(valueTwo).dividedBy(numOf(2));
    }
}
