
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.mitre.openaria.core.temp.Extras;
import org.mitre.openaria.core.temp.Extras.AircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasFlightRules;
import org.mitre.openaria.core.temp.Extras.HasSourceDetails;
import org.mitre.openaria.core.temp.Extras.SourceDetails;

/**
 * A NopEncoder converts Points into a raw text format that mimics the NOP RH Message format.
 * <p>
 * A NopEncoder cannot produce "full NOP RH messages" because the Point interface, does not
 * currently include certain NOP fields like AGW and STARS's scratchpad1 fields (as just one
 * example).
 */
public class NopEncoder {

    /*
     * This formatter helps convert and Instant to the Date format used within NOP
     */
    private static final SimpleDateFormat NOP_DATE_FORMAT = nopDateFormatter();

    public static SimpleDateFormat nopDateFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy,HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter;
    }

    /**
     * Note: Consider using a Point's "asNop()" method directly. It is possible the implementing
     * class can provide a better NOP encoding than a NopEncoder can generate. The NopEncoder must
     * compute the NOP encoding using only data available from the Point interface where as the
     * implementing class may have access to more data and therefore might be able to provide a more
     * complete NOP encoding.
     *
     * @param p A Point
     *
     * @return A String which has the "NOP encoding" of the provided point. Please know this is a
     *     "lossy" conversion because the Point interface does not include all the information that
     *     is available in any of the NOP formats (AGW, CENTER, or STARS)
     */
    public String asRawNop(Point p) {

        AircraftDetails acDetails = null;
        SourceDetails sourceDetails = null;
        String flightRules = null;
        String beaconAssigned = null;
        if (p instanceof HasAircraftDetails had) {
            acDetails = had.acDetails();
        }

        if (p instanceof HasSourceDetails hsd) {
            sourceDetails = hsd.sourceDetails();
        }

        if( p instanceof HasFlightRules hfr) {
            flightRules = hfr.flightRules();
        }

        if(p instanceof Extras.HasBeaconCodes hbc) {
            beaconAssigned = format(hbc.beaconAssigned());
        } else {
            beaconAssigned = format(null);
        }

        StringBuilder sb = new StringBuilder();
        String sensor = nonNull(sourceDetails) ? sourceDetails.sensor() : "";
        String facility = nonNull(sourceDetails) ? sourceDetails.facility() : "";

        String callsign = nonNull(acDetails) ? acDetails.callsign() : "";
        String acType = nonNull(acDetails) ? acDetails.aircraftType() : "";

        String DELIMITER = ",";

        sb.append("[RH]").append(DELIMITER)
            .append("STARS").append(DELIMITER) //assume STARS by convention
            .append(facility).append(DELIMITER) //token 2
            .append(formatTime(p.time())).append(DELIMITER) //tokens 3 and 4
            .append(callsign).append(DELIMITER) //token 5
            .append(acType).append(DELIMITER) //token 6
            .append(DELIMITER) //token 7 (STARS:equipmentTypeSuffix)
            .append(p.beaconActual()).append(DELIMITER) //token 8
            .append(formatAltitude(p)).append(DELIMITER) //token 9
            .append(formatSpeed(p)).append(DELIMITER) //token 10
            .append(formatCourse(p)).append(DELIMITER) //token 11
            .append(formatLatOrLong(p.latLong().latitude())).append(DELIMITER) //token 12
            .append(formatLatOrLong(p.latLong().longitude())).append(DELIMITER) // token 13
            .append(p.trackId()).append(DELIMITER) //token 14
            .append(beaconAssigned).append(DELIMITER) //token 15
            .append(DELIMITER) //token 16-AGW/STARS:X
            .append(DELIMITER) //token 17-AGW/STARS:Y
            .append(DELIMITER) //token 18-AGW/STARS:keyboard
            .append(DELIMITER) //token 19-AGW/STARS:positionSymbol, CENTER:controllingFacilitySector)
            .append(DELIMITER) //token 20-AGW/STARS:arrivalDepartureStatus lost here)
            .append(sensor).append(DELIMITER) //token 21???
            .append(DELIMITER) //token 22-AGW/STARS:scratchpad1
            .append(DELIMITER) //token 23-AGW/STARS:entryFix
            .append(DELIMITER) //token 24-AGW/STARS:exitFix
            .append(DELIMITER) //token 25-STARS:ptdTime, CENTER:coordinationTime
            .append(DELIMITER) //token 26-STARS:arrivalAirport
            .append(DELIMITER) //token 27-STARS:trackStatus
            .append(format(flightRules)).append(DELIMITER) //token 28
            .append(DELIMITER) //token 29-CENTER:cmsField153A
            .append(DELIMITER) //token 30-STARS:systemFlightPlanNumber, AGW:fdfNumber
            .append(DELIMITER) //token 31-AGW/CENTER:sequenceNumber
            .append(DELIMITER) //token 32-AGW/CENTER:departureAirport
            .append(DELIMITER) //token 33-CENTER:eta
            .append(DELIMITER) //token 34-CENTER:reportInterimAssignAltitude, AGW:sensor
            .append(DELIMITER) //token 35-AGW:scratchpad2
            .append(DELIMITER) //token 36-NOP heavyLargeOrSmall is lost here)
            .append(DELIMITER) //token 37-NOP onActiveSensor is lost here)
            .append(DELIMITER) //token 38-AGW:additionalFacilityAlphaChar
            .append(DELIMITER) //token 39 is unused
            .append("{RH}");

        return sb.toString();
    }

    public static String formatTime(Instant time) {
        Date date = new Date(time.toEpochMilli());
        return NOP_DATE_FORMAT.format(date);
    }

    private String formatLatOrLong(Double latOrLong) {
        checkNotNull(latOrLong, "Points must have both latitude and longitude");
        return String.format("%.5f", latOrLong); //use exactly 5 decimal points
    }

    private String formatXOrY(Double xOrY) {
        return (xOrY == null)
            ? ""
            : String.format("%.4f", xOrY);  //use exactly 4 decimal points
    }

    private String formatSpeed(Point p) {
        return (p.speedInKnots() == null)
            ? ""
            : Integer.toString(p.speedInKnots().intValue());
    }

    private String formatCourse(Point p) {
        return (p.course()) == null
            ? ""
            : Integer.toString(p.course().intValue());
    }

    private String formatAltitude(Point p) {

        return (p.altitude()) == null
            ? ""
            : Integer.toString((int) (p.altitude().inFeet() / 100));
    }

    /*
     * Prevent the String "null" from appearing in output encoding. Missing NOP fields are left
     * blank (i.e. "") so when we seek to duplicate NOP we must also return a blank
     */
    private String format(String s) {
        return (s == null) ? "" : s; //prevent "null" from appearing in output encoding
    }
}
