package org.mitre.openaria.core.formats.nop;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * This class contains simple text processing utilities that are used when processing raw NOP data.
 */
public class NopParsingUtils {

    static final DateTimeFormatter NOP_DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "MM/dd/yyyy HH:mm:ss.SSS X").withZone(ZoneOffset.UTC);

    private NopParsingUtils() {
        //prevent Object creation
    }

    public static String parseFacility(String token) {
        //parse first 3 characters to avoid backup facility "XXX_B"
        return token.substring(0, 3);
    }

    /**
     * Parse the two "time entries" from a Nop Message.
     *
     * @param dateString The "date portion" of a Nop Message. For example the "10/18/2016" in
     *                   "10/18/2016,00:57:12.962"
     * @param timeString The "time portion" of a Nop Message. For example the "00:57:12.962" in
     *                   "10/18/2016,00:57:12.962"
     *
     * @return The Instant corresponding to the date and time (Z time is assume)
     */
    public static Instant parseNopTime(String dateString, String timeString) {

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(
                dateString.replace("-", "/") + " " + timeString + " Z",
                NOP_DATE_FORMATTER
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
                return parseNopTime(dateString, correctedTimeString);
            } else {
                throw dtpe;
            }
        }
    }

    /*
     * This method makes it more obvious when a NOP message does not contain any information in its
     * comma delimited form. We don't want anyone assume an aircraftId of "" is valid.
     */
    public static String parseString(String token) {
        if (token.equals("")) {
            return null;
        } else {
            return token;
        }
    }

    public static Integer parseInteger(String token) {
        if (token.equals("")) {
            return null;
        } else {
            return Integer.parseInt(token);
        }
    }

    public static Double parseDouble(String x) {
        try {
            return Double.valueOf(x);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean parseBoolean(String token) {
        if (token.equals("0")) {
            return false;
        } else if (token.equals("1")) {
            return true;
        } else {
            return null;
        }
    }
}
