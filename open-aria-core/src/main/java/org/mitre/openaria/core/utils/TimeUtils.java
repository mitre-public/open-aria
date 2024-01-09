package org.mitre.openaria.core.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class TimeUtils {

    /**
     * Convert a duration of time to a String.
     *
     * @param duration A duration of time
     *
     * @return A String like "13:22:15" or "2 days, 13:22:15"
     */
    public static String asString(Duration duration) {

        long numDays = duration.toDays();
        long numHours = duration.toHours() % 24;
        long numMinutes = duration.toMinutes() % 60;
        long numSeconds = duration.getSeconds() % 60;

        String output = String.format("%d:%02d:%02d", numHours, numMinutes, numSeconds);

        if (numDays > 0L) {
            return numDays + " days, " + output;
        } else {
            return output;
        }
    }

    /**
     * Convert an Instant to a String that contains the date and time.
     *
     * @param instant A moment in time
     *
     * @return The above moment in 12/31/69 7:00 PM
     */
    public static String asString(Instant instant) {

        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

        return formatter.format(instant);
    }

    /**
     * Generate the date for a specific instant in time (assuming the UTC timezone).
     *
     * @param instant An instant in time. The date of this instant, in the UTC timezone, is
     *                provided.
     *
     * @return A String like "2017-03-27" (i.e. yyyy-mm-dd)
     */
    public static String utcDateAsString(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }


    /**
     * Generate the date for "right now". This date is set using the local system time.
     *
     * @return A String like "2017-03-27" (i.e. yyyy-mm-dd)
     */
    public static String todaysDateAsString() {
        /*
         * Note: I would have preferred to have this method accept an Instant as input. I was
         * prevented from doing this because Instants do not have time-zones, thus they don't map
         * cleanly into an exact Date. Consequently, I wrote the method that did exactly what I
         * needed (got me today's date in the format I wanted for logging purposes)
         */
        return (LocalDateTime.now()).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

}