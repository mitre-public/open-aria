package org.mitre.openaria.core;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;

import java.time.Instant;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.Speed;

/**
 * A PointBuilder provides a literate method to assemble Point data. For example, a PointBuilder
 * allows us to write:
 * <p>
 * Point p = (new PointBuilder()).sensor("mySensor").facility("myFacility").build(); <br>
 * {@code Map<PointField, Object>} data = (new PointBuilder()).sensor("mySensor").facility("myFacility");
 * <p>
 * Field setting methods come in two flavors. The first flavor: "callsign(String callsign)" requires
 * the field to not be set. This is for building Points from scratch. The second flavor
 * "butcallsign(String callsign)" overrides a previous setting and forcibly requires the field to
 * already be set. The second flavor of setter methods are typically used to quickly adjust existing
 * Points. This can be very useful for writing test cases or implementing point smoothing logic.
 * Here is an example:
 * <p>
 * Point p1 = (new PointBuilder(otherPoint)).butLatLong(newLatLong).build();
 * <p>
 * Notice, Point data can be converted to a CommonPoint object AND/OR a {@code Map<PointField, Object>}
 */
public class PointBuilder<T> {

    // Using Boxed types (not primitives) to help detect & reject unset values fields

    Long epochTime;

    Double latitude;

    Double longitude;

    Distance altitude;

    Speed speed;

    Course course;

    String trackId;

    T rawData;

    public PointBuilder() {
    }

    /**
     * Copy all the attributes of the input Point p
     *
     * @param p A Point
     */
    public PointBuilder(Point<T> p) {
        this();
        this.epochTime = p.timeAsEpochMs();
        this.latitude = p.latitude();
        this.longitude = p.longitude();
        this.altitude = p.altitude();
        this.speed = p.speed();
        this.course = p.course();
        this.trackId = p.trackId();
        this.rawData = p.rawData();
    }

    public PointBuilder<T> trackId(String trackId) {
        this.trackId = trackId;
        return this;
    }

    public PointBuilder<T> latLong(LatLong latitudeAndLongitude) {
        this.latitude = latitudeAndLongitude.latitude();
        this.longitude = latitudeAndLongitude.longitude();
        return this;
    }

    public PointBuilder<T> latLong(Double latitude, Double longitude) {
        checkLatitude(latitude);
        checkLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
        return this;
    }

    public PointBuilder<T> time(Instant time) {
        requireNonNull(time);
        this.epochTime = time.toEpochMilli();
        return this;
    }

    public PointBuilder<T> altitude(Distance altitude) {
        this.altitude = altitude;
        return this;
    }

    public PointBuilder<T> speed(Speed speed) {
        this.speed = speed;
        return this;
    }

    public PointBuilder<T> speedInKnots(double knots) {
        this.speed = Speed.ofKnots(knots);
        return this;
    }

    public PointBuilder<T> course(Course course) {
        this.course = course;
        return this;
    }

    public PointBuilder<T> courseInDegrees(double degrees) {
        this.course = Course.ofDegrees(degrees);
        return this;
    }

    public PointBuilder<T> rawData(T rawData) {
        this.rawData = rawData;
        return this;
    }


    public Point<T> build() {
        requireNonNull(epochTime, "Points must have a time");
        requireNonNull(latitude, "Points must have a latitude");
        requireNonNull(longitude, "Points must have a longitude");

        Position pos = new Position(
            Instant.ofEpochMilli(epochTime), LatLong.of(latitude, longitude), altitude);

        Velocity vel = (nonNull(speed) && nonNull(course))
            ? new Velocity(speed, course)
            : null;

        return new Point<>(pos, vel, trackId, rawData);
    }
}
