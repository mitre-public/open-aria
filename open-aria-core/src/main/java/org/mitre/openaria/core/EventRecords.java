
package org.mitre.openaria.core;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.mitre.openaria.core.temp.Extras.HasBeaconCodes;

/**
 * Contains that static rules used to format dates and times in EventRecords.
 */
public class EventRecords {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "MM/dd/yyyy,HH:mm:ss.SSS X").withZone(ZoneOffset.UTC);

    private static final SimpleDateFormat DATE_FORMAT = createFormater();

    private static SimpleDateFormat createFormater() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter;
    }

    /**
     * @param time An Instant written as a String in the format that is used in EventRecords. This
     *             String should be (A) human readable and (B) compatible with Property Files.
     *
     * @return The corresponding Instant
     */
    public static Instant parseTime(String time) {
        ZonedDateTime zdt = ZonedDateTime.parse(
            //			dateString.replace("-", "/") + " " + timeString + " Z",
            time + " Z",
            DATE_FORMATTER
        );
        return Instant.from(zdt);
    }

    /** Use this Beacon value, in place of null, when a Beacon code is unknown. */
    public static final String UNKOWN_BEACON_VALUE = "UNKNOWN";

    /**
     * @param point A Point that may or may not have a beacon code (actual beacon code, not assigned
     *              beacon code)
     *
     * @return The beacon code as a String, or UNKOWN_BEACON_VALUE when the Point's beacon code is
     *     null.
     */
    public static String safeBeaconCode(Point point) {

        System.out.println(point.getClass().getCanonicalName());

        if(point.rawData() instanceof HasBeaconCodes hbc) {

            String beacon = hbc.beaconActual();
            return (beacon == null)
                ? UNKOWN_BEACON_VALUE
                : beacon;
        }

        return UNKOWN_BEACON_VALUE;
    }
}
