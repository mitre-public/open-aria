

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Optional;

import org.mitre.openaria.core.NopPoints.AgwPoint;
import org.mitre.openaria.core.NopPoints.CenterPoint;
import org.mitre.openaria.core.NopPoints.MeartsPoint;
import org.mitre.openaria.core.NopPoints.StarsPoint;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.parsing.nop.AgwRadarHit;
import org.mitre.caasd.commons.parsing.nop.CenterRadarHit;
import org.mitre.caasd.commons.parsing.nop.MeartsRadarHit;
import org.mitre.caasd.commons.parsing.nop.NopMessage;
import org.mitre.caasd.commons.parsing.nop.NopMessageType;
import org.mitre.caasd.commons.parsing.nop.NopRadarHit;
import org.mitre.caasd.commons.parsing.nop.StarsRadarHit;

/**
 * A NopPoint is a Point implementation that wraps a NopRadarHit.
 * <p>
 * A NopPoint provides direct access to the underlying NopRadarHit. Having access to the NopRadarHit
 * permits creating custom logic (like point filtering and track smoothing) that relies on the
 * specific quirk of AGW, STARS, or CENTER data.
 *
 * @param <T> The type of NopRadarHit being wrapped
 */
public abstract class NopPoint<T extends NopRadarHit> implements Point {

    NopRadarHit rhMessage;

    NopPoint(String rawNopText) {
        checkNotNull(rawNopText, "The input String cannot be null");

        NopMessage m = NopMessageType.parse(rawNopText);

        checkArgument(
            m instanceof NopRadarHit,
            "Parsing the input String \"" + rawNopText
                + "\" resulted in a " + m.getClass().getName() + " but a NopRadarHit is required"
        );

        NopRadarHit message = (NopRadarHit) NopMessageType.parse(rawNopText);

        this.rhMessage = message;
    }

    NopPoint(T rhMessage) {
        this.rhMessage = checkNotNull(rhMessage);
    }

    public static NopPoint from(NopMessage message) {
        checkNotNull(message, "Cannot create a NopPoint from a null NopMessage");

        if (message instanceof CenterRadarHit) {
            return new CenterPoint((CenterRadarHit) message);
        } else if (message instanceof StarsRadarHit) {
            return new StarsPoint((StarsRadarHit) message);
        } else if (message instanceof AgwRadarHit) {
            return new AgwPoint((AgwRadarHit) message);
        } else if (message instanceof MeartsRadarHit) {
            return new MeartsPoint((MeartsRadarHit) message);
        } else {
            throw new IllegalArgumentException("Cannot create a NopPoint from a " + message.getNopType());
        }
    }

    public static NopPoint from(String rhMessage) {
        return from(NopMessageType.parse(rhMessage));
    }

    /**
     * This is a wrapped version of the other static factory methods. This factory method catches
     * any parsing exceptions and returns an empty Optional rather than throwing a Exception.
     */
    public static Optional<NopPoint> parseSafely(String rhMessage) {
        try {
            return Optional.of(from(rhMessage));
        } catch (Exception ex) {
            /*
             * Parsing exception have occured with bad lat/long data AND partially written points
             */
            return Optional.empty();
        }
    }


    @Override
    public String asNop() {
        return rhMessage.rawMessage();
    }

    @Override
    public String callsign() {
        return rhMessage.callSign();
    }

    @Override
    public String aircraftType() {
        return rhMessage.aircraftType();
    }

    @Override
    public abstract String trackId();

    @Override
    public String sensor() {
        return rhMessage.sensorIdLetters();
    }

    @Override
    public String facility() {
        return rhMessage.facility();
    }

    @Override
    public String beaconActual() {
        return rhMessage.reportedBeaconCode();
    }

    @Override
    public abstract String beaconAssigned();

    @Override
    public String flightRules() {
        return rhMessage.flightRules();
    }

    @Override
    public Distance altitude() {
        //NopRadarHit list altitude in increments of 100.  Thus 300 = 30,000ft
        return Distance.ofFeet(rhMessage.altitudeInHundredsOfFeet() * 100.0);
    }

    @Override
    public Double course() {
        return rhMessage.heading();
    }

    @Override
    public Double speedInKnots() {
        return rhMessage.speed();
    }

    @Override
    public Instant time() {
        return rhMessage.time();
    }

    @Override
    public Double curvature() {
        //the nop data format does not contain curvature information
        return null;
    }

    @Override
    public Double alongTrackDistance() {
        //the nop data format does not contain curvature information
        return null;
    }

    public T rawMessage() {
        return (T) this.rhMessage;
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(rhMessage.latitude(), rhMessage.longitude());
    }
}
