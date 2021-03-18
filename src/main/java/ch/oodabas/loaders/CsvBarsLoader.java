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
package ch.oodabas.loaders;

import com.opencsv.CSVReader;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class build a Ta4j bar series from a CSV file containing bars.
 */
public class CsvBarsLoader {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * @return the bar series from Apple Inc. bars.
     */
    private static BarSeries loadAppleIncSeries() {
        return loadCsvSeries("appleinc_bars_from_20130101_usd.csv");
    }

    private static BarSeries loadCsvSeries(final String filename) {

        final InputStream stream = CsvBarsLoader.class.getClassLoader().getResourceAsStream(filename);

        final BarSeries series = new BaseBarSeries("apple_bars");

        try {
            assert stream != null;
            try (final CSVReader csvReader = new CSVReader(new InputStreamReader(stream, StandardCharsets.UTF_8), ',', '"',
                    1)) {
                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    final ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
                    final double open = Double.parseDouble(line[1]);
                    final double high = Double.parseDouble(line[2]);
                    final double low = Double.parseDouble(line[3]);
                    final double close = Double.parseDouble(line[4]);
                    final double volume = Double.parseDouble(line[5]);

                    series.addBar(date, open, high, low, close, volume);
                }
            }
        } catch (final IOException ioe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Unable to load bars from CSV", ioe);
        } catch (final NumberFormatException nfe) {
            Logger.getLogger(CsvBarsLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }
        return series;
    }

    public static void main(final String[] args) {
        final BarSeries series = CsvBarsLoader.loadAppleIncSeries();

        System.out.println("Series: " + series.getName() + " (" + series.getSeriesPeriodDescription() + ")");
        System.out.println("Number of bars: " + series.getBarCount());
        System.out.println("First bar: \n" + "\tVolume: " + series.getBar(0).getVolume() + "\n" + "\tOpen price: "
                + series.getBar(0).getOpenPrice() + "\n" + "\tClose price: " + series.getBar(0).getClosePrice());
    }
}