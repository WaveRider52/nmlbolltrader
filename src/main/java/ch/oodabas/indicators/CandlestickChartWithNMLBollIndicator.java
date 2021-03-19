package ch.oodabas.indicators;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.bollinger.PercentBIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * This class builds a traditional candlestick chart.
 */
public class CandlestickChartWithNMLBollIndicator {
    private static final DateAxis xAxis = new DateAxis("Time");

    public static void main(final String[] args) {

        final BarSeries series = readCsv("Binance.com_1m_ETHBTC_21_33_25-2-2021.csv");

        assert series != null;
        final OHLCDataset ohlcDataset = createOHLCDataset(series);

        final TimeSeriesCollection bollDataset = createBollDataset(series);

        final TimeSeriesCollection pcbDataset = createPCBDataset(series);

        displayChart(ohlcDataset, bollDataset, pcbDataset);
    }

    /**
     * Builds a JFreeChart OHLC dataset from a ta4j bar series.
     *
     * @param series a bar series
     * @return an Open-High-Low-Close dataset
     */
    private static OHLCDataset createOHLCDataset(final BarSeries series) {
        final int nbBars = series.getBarCount();

        final Date[] dates = new Date[nbBars];
        final double[] opens = new double[nbBars];
        final double[] highs = new double[nbBars];
        final double[] lows = new double[nbBars];
        final double[] closes = new double[nbBars];
        final double[] volumes = new double[nbBars];

        for (int i = 0; i < nbBars; i++) {
            final Bar bar = series.getBar(i);
            dates[i] = new Date(bar.getEndTime().toEpochSecond() * 1000);
            opens[i] = bar.getOpenPrice().doubleValue();
            highs[i] = bar.getHighPrice().doubleValue();
            lows[i] = bar.getLowPrice().doubleValue();
            closes[i] = bar.getClosePrice().doubleValue();
            volumes[i] = bar.getVolume().doubleValue();
        }

        return new DefaultHighLowDataset("btc", dates, highs, lows, opens, closes, volumes);
    }

    private static TimeSeriesCollection createBollDataset(final BarSeries series) {

        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator sma = new SMAIndicator(closePriceIndicator, 20);

        final StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePriceIndicator, 20);


        final BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        final BollingerBandsUpperIndicator bbuSMA = new BollingerBandsUpperIndicator(bbmSMA, standardDeviation);
        final BollingerBandsLowerIndicator bblSMA = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation);
        final BollingerBandsLowerIndicator nmlUP = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(-1));
        final BollingerBandsLowerIndicator nmlLO = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation,
                PrecisionNum.valueOf(1));

        /*
         * Building chart dataset
         */
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(buildChartBarSeries(series, bbuSMA, "High Bollinger Band"));
        dataset.addSeries(buildChartBarSeries(series, nmlUP, "NML Upper"));

        dataset.addSeries(buildChartBarSeries(series, nmlLO, "NML Lower"));
        dataset.addSeries(buildChartBarSeries(series, bblSMA, "Low Bollinger Band"));

        return dataset;

    }

    private static TimeSeriesCollection createPCBDataset(final BarSeries series) {

        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        OpenPriceIndicator openPriceIndicator = new OpenPriceIndicator(series);

//        final PercentBIndicator pcb = new PercentBIndicator(closePriceIndicator, 20, -1);
        DoublePercentBIndicator doublePercentBIndicator = new DoublePercentBIndicator(openPriceIndicator, closePriceIndicator, series);
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(buildChartBarSeries(series, doublePercentBIndicator, "PCB"));

        return dataset;
    }

    private static org.jfree.data.time.TimeSeries buildChartBarSeries(final BarSeries barSeries, final Indicator<Num> indicator,
                                                                      final String name) {
        final org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < barSeries.getBarCount(); i++) {
            final Bar bar = barSeries.getBar(i);

            chartTimeSeries.add(new Second(new Date(bar.getEndTime().toEpochSecond() * 1000)),
                    indicator.getValue(i).doubleValue());
        }
        return chartTimeSeries;
    }

    private static void displayChart(final XYDataset ohlcDataset, final XYDataset chopSeries, final TimeSeriesCollection pcbDataset) {
        /*
         * Create the chart
         */
        final CandlestickRenderer renderer = new CandlestickRenderer();
        final XYPlot pricePlot = new XYPlot(ohlcDataset, xAxis, new NumberAxis("Price"), renderer);
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);

        final NumberAxis numberAxis = (NumberAxis) pricePlot.getRangeAxis();

        // Misc
        pricePlot.setRangeGridlinePaint(Color.lightGray);
        pricePlot.setBackgroundPaint(Color.white);
        numberAxis.setAutoRangeIncludesZero(false);
        pricePlot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        final XYLineAndShapeRenderer renderer3 = new XYLineAndShapeRenderer(true, false);

        renderer3.setSeriesPaint(0, Color.ORANGE);
        renderer3.setSeriesPaint(1, Color.GRAY);
        renderer3.setSeriesPaint(2, Color.GRAY);
        renderer3.setSeriesPaint(3, Color.BLUE);
        renderer3.setSeriesPaint(4, Color.RED);
        pricePlot.setRenderer(1, renderer3);
        pricePlot.setDataset(1, chopSeries);
        pricePlot.mapDatasetToRangeAxis(1, 0);

        // secondary study plot
        final XYPlot indicatorXYPlot = new XYPlot();
        indicatorXYPlot.setDataset(pcbDataset);
        indicatorXYPlot.setRangeAxis(0, new NumberAxis(""));
        indicatorXYPlot.setRenderer(0, new XYLineAndShapeRenderer());
        final NumberAxis yIndicatorAxis = new NumberAxis("");
        yIndicatorAxis.setRange(0, 2);
        indicatorXYPlot.setRangeAxis(0, yIndicatorAxis);


        // combinedPlot
        final CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis); // DateAxis
        combinedPlot.setGap(10.0);
        // combinedPlot.setDomainAxis( xAxis );
        combinedPlot.setBackgroundPaint(Color.LIGHT_GRAY);
        combinedPlot.setDomainGridlinePaint(Color.GRAY);
        combinedPlot.setRangeGridlinePaint(Color.GRAY);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);
        combinedPlot.add(pricePlot, 70);
        combinedPlot.add(indicatorXYPlot, 30);

        // Now create the chart that contains the combinedPlot
        final JFreeChart combinedChart = new JFreeChart("BTC price with No Man's Land & Bollinger indicator", null, combinedPlot, true);
        combinedChart.setBackgroundPaint(Color.LIGHT_GRAY);

        // combinedChartPanel to contain combinedChart
        final ChartPanel combinedChartPanel = new ChartPanel(combinedChart);
        combinedChartPanel.setLayout(new GridLayout(0, 1));
        combinedChartPanel.setBackground(Color.LIGHT_GRAY);
        combinedChartPanel.setPreferredSize(new java.awt.Dimension(740, 300));

        // Application frame
        final ApplicationFrame frame = new ApplicationFrame("Candlestick chart");
        frame.setContentPane(combinedChartPanel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    private static BarSeries readCsv(final String activeFile) {
        final File fileFromResource;
        try {
            fileFromResource = getFileFromResource(activeFile);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        final BarSeries serie = new BaseBarSeriesBuilder().withNumTypeOf(PrecisionNum::valueOf).build();
        try (final BufferedReader br = new BufferedReader(new FileReader(fileFromResource))) {
            String line;
            br.readLine();// skip header
            while ((line = br.readLine()) != null) {
                final String[] values = line.split(",");
                final ZonedDateTime zonedDateTime = ZonedDateTime.parse(values[0]);
                final double open = Double.parseDouble(values[1]);
                final double high = Double.parseDouble(values[2]);
                final double low = Double.parseDouble(values[3]);
                final double close = Double.parseDouble(values[4]);
                final double vol = Double.parseDouble(values[5]);
                serie.addBar(zonedDateTime, open, high, low, close, vol);
            }
        } catch (final IOException e) {
            e.printStackTrace();

        }
        return serie;
    }

    private static File getFileFromResource(final String fileName) throws URISyntaxException {

        final ClassLoader classLoader = CandlestickChartWithNMLBollIndicator.class.getClassLoader();
        final URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return new File(resource.toURI());
        }
    }
}
