package org.mitre.openaria.core;

import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.PositionRecord;

/**
 * A CsvPositioner builds a {@code PositionRecord<String>} by parsing a String of comma-separated
 * data.
 */
public class CsvPositioner implements Positioner<String> {

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd,HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    private final int latitudeColumn;

    private final int longitudeColumn;

    private final int altitudeColumn;

    private final int dateColumn;

    private final int timeColumn;


    /**
     * Create a CsvPositioner that expects: date, time, latitude, longitude, and altitude in columns
     * 2, 3, 4, 5, and 6 respectively.  This "default parsing configuration" ignores columns 0 and 1
     * to allow CSV Strings to be alphabetically sorted by a field that is NOT simply date.
     * <p>
     * Example input: ",,YYYY-MM-DD,HH:mm:ss.SSS,LAT,LONG,ALT_IN_FT, ... many additional fields"
     */
    public CsvPositioner() {
        this(2, 3, 4, 5, 6);
    }

    /**
     * Create a CsvPositioner that expects specific fields in specific CSV column positions
     *
     * @param dateCol The index of the column containing the date "YYYY-MM-DD"
     * @param timeCol The index of the column containing the time "HH:mm:ss.SSS" (Z-time is
     *                assumed)
     * @param latCol  The index of the column containing the latitude
     * @param longCol The index of the column containing the longitude
     * @param altCol  The index of the column containing the altitude (in feet)
     */
    public CsvPositioner(int dateCol, int timeCol, int latCol, int longCol, int altCol) {
        this.latitudeColumn = latCol;
        this.longitudeColumn = longCol;
        this.altitudeColumn = altCol;
        this.timeColumn = timeCol;
        this.dateColumn = dateCol;
    }


    /**
     * @param rawCsv A String of comma-separated data.  This entire input String is retained within
     *               the {@code PositionRecord<String>}
     *
     * @return A PositionRecord that pairs the {@code Position} we can derive from this CSV String
     *     with the raw CSV String itself.  Thus, the output of this method does not hide access to
     *     the raw input.
     */
    @Override
    public PositionRecord<String> asRecord(String rawCsv) {

        Position pos = parsePositionFrom(rawCsv);

        return PositionRecord.of(rawCsv, pos);
    }


    /**
     * Find the Time, Latitude, Longitude, and Altitude from within this CSV string.
     *
     * @return The Position embedded within this row of CSV data
     */
    public Position parsePositionFrom(String rawCsv) {

        String[] tokens = rawCsv.split(",");

        Instant time = parseTime(tokens[dateColumn], tokens[timeColumn]);

        double latitude = Double.parseDouble(tokens[latitudeColumn]);
        double longitude = Double.parseDouble(tokens[longitudeColumn]);
        checkLatitude(latitude);
        checkLongitude(longitude);

        double altitudeInFeet = Double.parseDouble(tokens[altitudeColumn]);

        return new Position(
            time,
            LatLong.of(latitude, longitude),
            Distance.ofFeet(altitudeInFeet)
        );
    }

    /**
     * Combine the "dateString" and "timeString" to form and Instant.
     *
     * @param dateString The "date portion" from a two column timestamp. E.g., the "2016-10-18" in
     *                   "2016-10-18,00:57:12.962". Supports both "YYYY-MM-DD" and "YYYY/MM/DD"
     * @param timeString The "time portion" from a two column timestamp. E.g., the "00:57:12.962" in
     *                   "2016-10-18,00:57:12.962"
     *
     * @return The Instant corresponding to the date and time (Z time is assumed)
     */
    public static Instant parseTime(String dateString, String timeString) {

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(
                dateString.replace("/", "-") + "," + timeString,
                DATE_FORMATTER
            );
            return Instant.from(zdt);

        } catch (DateTimeParseException dtpe) {
            /*
             * In rare cases the date is incorrectly written: For example, here is a sequence of 3
             * date and time values: "10/18/2016,00:57:12.962", "10/18/2016,00:57:12.992", and
             * "10/18/2016,00:57:121.00" [this 3rd example has an error]
             *
             * Here the milliseconds portion of the "time" token is "1.00" instead of ".999". The
             * code below corrects this one possible error, all other parsing issues are re-thrown.
             */
            if (timeString.endsWith("1.00")) {
                //replace the "1.00" String with ".999"
                String correctedTimeString = timeString.substring(
                    0,
                    timeString.lastIndexOf("1.00")
                ) + ".999";
                return parseTime(dateString, correctedTimeString);
            } else {
                throw dtpe;
            }
        }
    }

    /** Generate a correctly formatted "Date,Time" String the default parser would expect. */
    public static String formatTime(Instant time) {
        return DATE_FORMATTER.format(time);
    }
}
