package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.core.temp.Extras.BeaconCodes;
import static org.mitre.openaria.core.temp.Extras.HasBeaconCodes;

import java.time.Instant;
import java.util.Optional;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.parsing.nop.AgwRadarHit;
import org.mitre.caasd.commons.parsing.nop.CenterRadarHit;
import org.mitre.caasd.commons.parsing.nop.MeartsRadarHit;
import org.mitre.caasd.commons.parsing.nop.NopMessage;
import org.mitre.caasd.commons.parsing.nop.NopMessageType;
import org.mitre.caasd.commons.parsing.nop.NopRadarHit;
import org.mitre.caasd.commons.parsing.nop.StarsRadarHit;
import org.mitre.openaria.core.NopPoints.AgwPoint;
import org.mitre.openaria.core.NopPoints.CenterPoint;
import org.mitre.openaria.core.NopPoints.MeartsPoint;
import org.mitre.openaria.core.NopPoints.StarsPoint;
import org.mitre.openaria.core.temp.Extras.AircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasFlightRules;
import org.mitre.openaria.core.temp.Extras.HasSourceDetails;
import org.mitre.openaria.core.temp.Extras.SourceDetails;

/**
 * A NopPoint is a Point implementation that wraps a NopRadarHit.
 * <p>
 * A NopPoint provides direct access to the underlying NopRadarHit. Having access to the NopRadarHit
 * permits creating custom logic (like point filtering and track smoothing) that relies on the
 * specific quirk of AGW, STARS, or CENTER data.
 *
 * @param <T> The type of NopRadarHit being wrapped
 */
public abstract class NopPoint<T extends NopRadarHit> implements Point<String>, HasSourceDetails, HasAircraftDetails, HasFlightRules, HasBeaconCodes {

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
    
    public String rawData() {
        return this.rhMessage.rawMessage();
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
    public abstract String trackId();

    @Override
    public String beaconActual() {
        return rhMessage.reportedBeaconCode();
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

    public T rawMessage() {
        return (T) this.rhMessage;
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(rhMessage.latitude(), rhMessage.longitude());
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
}
