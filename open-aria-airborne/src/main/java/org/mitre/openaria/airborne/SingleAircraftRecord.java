package org.mitre.openaria.airborne;

import static java.lang.Math.round;
import static org.mitre.openaria.core.EventRecords.safeBeaconCode;

import java.time.Duration;
import java.time.Instant;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.openaria.core.IfrVfrAssigner;
import org.mitre.openaria.core.IfrVfrStatus;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.metadata.Direction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Contains data about exactly one aircraft.
 *
 * <p>This class is intended to be written to Json using serialization tools.
 */
public class SingleAircraftRecord implements JsonWritable {

    /* The converter is static so that it can be reused. Relection is expensive. */
    private static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();


    final String callsign;
    private final String uniqueId;
    private final int altitudeInFeet;
    private final String climbStatus;
    private final int speedInKnots;
    private final Direction direction;
    private final int course;
    private final String beaconcode;
    final String trackId;
    final String aircraftType;
    private final double latitude;
    private final double longitude;
    private final IfrVfrStatus ifrVfrStatus;
    final int climbRateInFeetPerMin;

    public SingleAircraftRecord(Track<?> track, Instant eventTime, String trackHash) {
        Point p = track.interpolatedPoint(eventTime).get();
        this.callsign = track.callsign();
        this.uniqueId = trackHash;
        this.speedInKnots = (int) p.speed().inKnots();
        this.trackId = track.trackId();
        this.aircraftType = track.aircraftType();
        this.latitude = p.latLong().latitude();
        this.longitude = p.latLong().longitude();
        this.beaconcode = safeBeaconCode(p);
        this.ifrVfrStatus = (new IfrVfrAssigner()).statusOf(track, eventTime);
        this.altitudeInFeet = (int) p.altitude().inFeet();
        this.course = (int) round(p.course().inDegrees());
        this.direction = Direction.approxDirectionOf(p.course().inDegrees());
        Speed climbRate = computeClimbRate(track, eventTime);
        this.climbRateInFeetPerMin = (int) Math.round(climbRate.inFeetPerMinutes());
        this.climbStatus = climbStatus(climbRate);
    }

    public String trackId() {
        return trackId;
    }

    public String beaconCode() {
        return beaconcode;
    }

    public String callsign() {
        return callsign;
    }

    public IfrVfrStatus ifrVfrStatus() {
        return this.ifrVfrStatus;
    }

    public Distance altitude() {
        return Distance.ofFeet(this.altitudeInFeet);
    }

    public String climbStatus() {
        return this.climbStatus;
    }

    public Course course() {
        return Course.ofDegrees(this.course);
    }

    @Override
    public String asJson() {
        return GSON_CONVERTER.toJson(this);
    }

    public static SingleAircraftRecord parseJson(String json) {
        return GSON_CONVERTER.fromJson(json, SingleAircraftRecord.class);
    }

    private static String climbStatus(Speed climbRate) {
        if (climbRate.isZero()) {
            return "LEVEL";
        } else if (climbRate.isPositive()) {
            return "CLIMBING";
        } else if (climbRate.isNegative()) {
            return "DESCENDING";
        }
        throw new IllegalStateException("Could not convert a Speed to a ClimbStatus");
    }

    static Speed computeClimbRate(Track<?> track, Instant time) {

        Duration timeDelta = Duration.ofMillis(7_500);

        Instant priorMoment = time.minus(timeDelta);
        Instant laterMoment = time.plus(timeDelta);

        Point priorPoint = track.interpolatedPoint(priorMoment).orElse(null); //will be null at beginning of track
        Point point = track.interpolatedPoint(time).get();
        Point laterPoint = track.interpolatedPoint(laterMoment).orElse(null); //will be null at end of track

        if (priorPoint != null && laterPoint != null) {

            //we are in the middle of the track and the climbrate can reflect forward and backward altitude values
            Speed sample1 = calculateClimbRate(priorPoint, point);
            Speed sample2 = calculateClimbRate(priorPoint, laterPoint);
            Speed sample3 = calculateClimbRate(point, laterPoint);
            return mean(sample1, sample2, sample3);

        } else if (priorPoint == null && laterPoint == null) {

            //the track is VERY small -- you run off the track overlap both forward and backward in time
            //consequently, report a climbrate that reflect the average climb rate of this short time duration.
            TimeWindow window = track.asTimeWindow();

            Point startPoint = track.interpolatedPoint(window.start()).get();
            Point endPoint = track.interpolatedPoint(window.end()).get();
            return calculateClimbRate(startPoint, endPoint);

        } else if (priorPoint != null && laterPoint == null) {
            //at the very end of the track
            return calculateClimbRate(priorPoint, point);

        } else if (priorPoint == null && laterPoint != null) {
            //at the very beginning of the track
            return calculateClimbRate(point, laterPoint);

        } else {
            throw new AssertionError("This case should be illegal");
        }
    }


    static Speed calculateClimbRate(Point p1, Point p2) {
        Distance altitudeDelta = p2.altitude().minus(p1.altitude());
        Duration timeDelta = Duration.between(p1.time(), p2.time());
        return altitudeDelta.dividedBy(timeDelta);
    }

    //@todo  -- This method should be moved somewhere else..
    static Speed mean(Speed... speeds) {
        double n = speeds.length;

        Speed spd = Speed.ZERO;
        for (Speed speed : speeds) {
            spd = spd.plus(speed);
        }
        return spd.times(1.0 / n);
    }
}
