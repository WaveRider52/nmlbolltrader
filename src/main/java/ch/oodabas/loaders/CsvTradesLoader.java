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
import org.ta4j.core.*;
import org.ta4j.core.num.PrecisionNum;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class builds a Ta4j bar series from a CSV file containing trades.
 */
public class CsvTradesLoader {

    // 2021-02-25T21:33+01:00[Europe/Zurich]
 /*   DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
 OffsetDateTime.parse("2019-08-13T07:29:12.000+0000", formatter);
 DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'+' z");
 DateTimeFormatter.ISO_OFFSET_DATE_TIME;*/
    private static final DateTimeFormatter DATE_FORMAT_HOURLY_MINUTE = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    //ofPattern("yyyy-MM-dd'T'HH:mm'Z'");


    public static BarSeries readCsv(final String activeFile) {
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

        final ClassLoader classLoader = CsvTradesLoader.class.getClassLoader();
        final URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {

            // failed if files have whitespaces or special characters
            //return new File(resource.getFile());

            return new File(resource.toURI());
        }

    }

    /**
     * @return the bar series from Bitstamp (bitcoin exchange) trades
     */
    public static BarSeries loadBitstampSeries() {

        /* Reading all lines of the CSV file
         Binance.com_1m_ETHBTC_21_33_25-2-2021.csv
         bitstamp_trades_from_20131125_usd.csv
        */
        final InputStream stream = CsvTradesLoader.class.getClassLoader()
                .getResourceAsStream("Binance.com_1m_ETHBTC_21_33_25-2-2021.csv");
        CSVReader csvReader = null;
        List<String[]> lines = null;
        try {
            assert stream != null;
            csvReader = new CSVReader(new InputStreamReader(stream, StandardCharsets.UTF_8), ',');
            lines = csvReader.readAll();
            lines.remove(0); // Removing header line
        } catch (final IOException ioe) {
            Logger.getLogger(CsvTradesLoader.class.getName()).log(Level.SEVERE, "Unable to load trades from CSV", ioe);
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (final IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        final BarSeries series = new BaseBarSeries();
        if ((lines != null) && !lines.isEmpty()) {

            // Getting the first and last trades timestamps
//// 2021-02-25T21:33+01:00[Europe/Zurich] (Harald's CSV)
            ZonedDateTime beginTime = ZonedDateTime.parse(lines.get(0)[0], DATE_FORMAT_HOURLY_MINUTE);
            ZonedDateTime endTime = ZonedDateTime.parse(lines.get(lines.size() - 1)[0], DATE_FORMAT_HOURLY_MINUTE);
            if (beginTime.isAfter(endTime)) {
                final Instant beginInstant = beginTime.toInstant();
                final Instant endInstant = endTime.toInstant();
                beginTime = ZonedDateTime.ofInstant(endInstant, ZoneId.systemDefault());
                endTime = ZonedDateTime.ofInstant(beginInstant, ZoneId.systemDefault());
                // Since the CSV file has the most recent trades at the top of the file, we'll
                // reverse the list to feed
                // the List<Bar> correctly.
                Collections.reverse(lines);
            }
            // build the list of populated bars
            buildSeries(series, beginTime, endTime, 60, lines);
        }

        return series;
    }

    /**
     * Builds a list of populated bars from csv data.
     *
     * @param beginTime the begin time of the whole period
     * @param endTime   the end time of the whole period
     * @param duration  the bar duration (in seconds)
     * @param lines     the csv data returned by CSVReader.readAll()
     */
    private static void buildSeries(final BarSeries series, final ZonedDateTime beginTime, final ZonedDateTime endTime, final int duration,
                                    final List<String[]> lines) {

        final Duration barDuration = Duration.ofSeconds(duration);
        ZonedDateTime barEndTime = beginTime;
        // line number of trade data
        int i = 0;
        do {
            // build a bar
            barEndTime = barEndTime.plus(barDuration);
            final Bar bar = new BaseBar(barDuration, barEndTime, series.function());
            do {
                // get a trade
                final String[] tradeLine = lines.get(i);
                final ZonedDateTime tradeTimeStamp = ZonedDateTime.parse(tradeLine[0], DATE_FORMAT_HOURLY_MINUTE);
                // if the trade happened during the bar
                if (bar.inPeriod(tradeTimeStamp)) {
                    // add the trade to the bar
                    final double tradePrice = Double.parseDouble(tradeLine[1]);
                    final double tradeVolume = Double.parseDouble(tradeLine[2]);
                    bar.addTrade(tradeVolume, tradePrice, series.function());
                } else {
                    // the trade happened after the end of the bar
                    // go to the next bar but stay with the same trade (don't increment i)
                    // this break will drop us after the inner "while", skipping the increment
                    break;
                }
                i++;
            } while (i < lines.size());
            // if the bar has any trades add it to the bars list
            // this is where the break drops to
            if (bar.getTrades() > 0) {
                series.addBar(bar);
            }
        } while (barEndTime.isBefore(endTime));
    }

    public static void main(final String[] args) {
        final BarSeries series = CsvTradesLoader.loadBitstampSeries();

        System.out.println("Series: " + series.getName() + " (" + series.getSeriesPeriodDescription() + ")");
        System.out.println("Number of bars: " + series.getBarCount());
        System.out.println("First bar: \n" + "\tVolume: " + series.getBar(0).getVolume() + "\n" + "\tNumber of trades: "
                + series.getBar(0).getTrades() + "\n" + "\tClose price: " + series.getBar(0).getClosePrice());
    }
}