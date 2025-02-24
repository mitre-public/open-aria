package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.core.temp.Extras.BeaconCodes;
import static org.mitre.openaria.core.temp.Extras.HasBeaconCodes;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Velocity;
import org.mitre.openaria.core.temp.Extras.AircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasFlightRules;
import org.mitre.openaria.core.temp.Extras.HasSourceDetails;
import org.mitre.openaria.core.temp.Extras.SourceDetails;

/**
 * A NopHit wraps a raw NopRadarHit and declares which fields it contains using Trait interfaces.
 * <p>
 * NopHit provides direct access to the underlying NopRadarHit. Having access to the NopRadarHit
 * permits creating custom logic (like point filtering and track smoothing) that relies on the
 * specific quirk of AGW, STARS, or CENTER data.
 */
public class NopHit implements HasSourceDetails, HasAircraftDetails, HasFlightRules, HasBeaconCodes, HasTime {

    private final NopRadarHit rhMessage;

    public NopHit(String rawNopText) {
        checkNotNull(rawNopText, "The input String cannot be null");

        NopMessage m = NopMessageType.parse(rawNopText);

        checkArgument(
            m instanceof NopRadarHit,
            "Parsing the input String \"" + rawNopText
                + "\" resulted in a " + m.getClass().getName() + " but a NopRadarHit is required"
        );

        this.rhMessage = (NopRadarHit) NopMessageType.parse(rawNopText);
    }

    public NopHit(NopRadarHit rhMessage) {
        this.rhMessage = checkNotNull(rhMessage);
    }

    public static Point<NopHit> from(NopMessage message) {
        checkNotNull(message, "Cannot create a NopPoint from a null NopMessage");

        NopHit wrapped = null;

        if (message instanceof CenterRadarHit center) {
            wrapped = new NopHit(center);
        } else if (message instanceof StarsRadarHit stars) {
            wrapped = new NopHit(stars);
        } else if (message instanceof AgwRadarHit agw) {
            wrapped = new NopHit(agw);
        } else if (message instanceof MeartsRadarHit mearts) {
            wrapped = new NopHit(mearts);
        } else {
            throw new IllegalArgumentException("Cannot create a NopPoint from a " + message.getNopType());
        }

        return new Point<>(wrapped.position(), wrapped.velocity(), wrapped.trackId(), wrapped);
    }

    public static Point<NopHit> from(String rhMessage) {
        return from(NopMessageType.parse(rhMessage));
    }

    /**
     * This is a wrapped version of the other static factory methods. This factory method catches
     * any parsing exceptions and returns an empty Optional rather than throwing an Exception.
     */
    public static Optional<Point<NopHit>> parseSafely(String rhMessage) {
        try {
            return Optional.of(from(rhMessage));
        } catch (Exception ex) {
            /*
             * Parsing exception have occured with bad lat/long data AND partially written points
             */
            return Optional.empty();
        }
    }

    public String trackId() {

        if (rawMessage() instanceof AgwRadarHit agw) {
            return agw.trackNumber();
        }

        if (rawMessage() instanceof StarsRadarHit stars) {
            return stars.trackNumber();
        }

        if (rawMessage() instanceof CenterRadarHit center) {
            return center.computerId();
        }

        if (rawMessage() instanceof MeartsRadarHit mearts) {
            return mearts.computerId();
        }

        throw new AssertionError("Illegal case");
    }

    @Override
    public String beaconActual() {
        return rhMessage.reportedBeaconCode();
    }


    @Override
    public String beaconAssigned() {

        if (rawMessage() instanceof AgwRadarHit agw) {

            return (agw.assignedBeaconCode() == null)
                ? null
                /*
                 * when converting the Integer to a String be sure to intern the resulting String so
                 * that you don't generate hundreds of separate copies of the beacon code
                 */
                : agw.assignedBeaconCode().toString().intern();
        }

        if (rawMessage() instanceof StarsRadarHit stars) {

            return (stars.assignedBeaconCode() == null)
                ? null
                /*
                 * when converting the Integer to a String be sure to intern the resulting String so
                 * that you don't generate hundreds of separate copies of the beacon code
                 */
                : stars.assignedBeaconCode().toString().intern();
        }

        if (rawMessage() instanceof CenterRadarHit center) {

            //these Center format does not contain this information
            return null;
        }

        if (rawMessage() instanceof MeartsRadarHit mearts) {

            //these Mearts format does not contain this information
            return null;
        }

        throw new AssertionError("Illegal case");
    }

    @Override
    public String flightRules() {
        return rhMessage.flightRules();
    }

    public boolean hasFlightRules() {
        return !flightRulesIsMissing();
    }

    public boolean flightRulesIsMissing() {
        String rules = this.flightRules();
        return (rules == null || rules.equals(""));
    }

    public Distance altitude() {
        //NopRadarHit list altitude in increments of 100 (i.e., 300 = 30,000ft)
        return Distance.ofFeet(rhMessage.altitudeInHundredsOfFeet() * 100.0);
    }

    public Double course() {
        return rhMessage.heading();
    }

    public Double speedInKnots() {
        return rhMessage.speed();
    }

    public Velocity velocity() {
        try {
            return new Velocity(
                Speed.ofKnots(speedInKnots()),
                Course.ofDegrees(course())
            );
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Instant time() {
        return rhMessage.time();
    }

    public NopRadarHit rawMessage() {
        return this.rhMessage;
    }

    public LatLong latLong() {
        return LatLong.of(rhMessage.latitude(), rhMessage.longitude());
    }

    public Position position() {
        return new Position(time(), latLong(), altitude());
    }

    @Override
    public SourceDetails sourceDetails() {
        return new SourceDetails(rhMessage.sensorIdLetters(), rhMessage.facility());
    }

    @Override
    public AircraftDetails acDetails() {
        return new AircraftDetails(rhMessage.callSign(), rhMessage.aircraftType());
    }

    @Override
    public BeaconCodes beaconCodes() {
        return new BeaconCodes(rhMessage.reportedBeaconCode(), "");
    }

    public boolean hasValidBeaconActual() {
        return !beaconActualIsMissing();
    }

    public boolean beaconActualIsMissing() {
        String beacon = beaconActual();
        return (beacon == null || beacon.equals("") || beacon.equals("null"));
    }

    public int beaconActualAsInt() {
        return Integer.parseInt(beaconActual());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        NopHit nopHit = (NopHit) o;

        return Objects.equals(rhMessage, nopHit.rhMessage);
    }

    @Override
    public int hashCode() {
        return rhMessage != null ? rhMessage.hashCode() : 0;
    }
}
