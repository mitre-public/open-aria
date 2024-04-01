

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

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
public class PointBuilder {

    HashMap<PointField, Object> data;

    public PointBuilder() {
        this.data = new HashMap<>();
    }

    /**
     * Copy all the attributes of the input Point p
     *
     * @param p A Point
     */
    public PointBuilder(Point p) {
        this();
        this.addAll(Points.toMap(p));
    }

    /**
     * Set a single field of a Point under construction. The same field cannot be set twice.
     *
     * @param field The Field to set
     * @param value The value to set
     *
     * @return This PointBuilder (to allow method chaining)
     */
    private PointBuilder set(PointField field, Object value) {

        checkArgument(field.accepts(value), field + " does not accept this value type");

        if (data.containsKey(field)) {
            throw new IllegalStateException("The field: " + field + " was already set");
        }

        if (field.expectedType == String.class) {
            if ("".equals(value)) {
                throw new IllegalArgumentException(
                    "Cannot assign the empty String to the field: " + field + ".  "
                        + "Null is prefered in this case for clarity.");
            }
        }

        data.put(field, value);
        return this;
    }

    public PointBuilder addAll(Map<PointField, Object> map) {
        for (Map.Entry<PointField, Object> entry : map.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public PointBuilder callsign(String callsign) {
        return set(PointField.CALLSIGN, callsign);
    }

    public PointBuilder aircraftType(String aircraftType) {
        return set(PointField.AIRCRAFT_TYPE, aircraftType);
    }

    public PointBuilder trackId(String trackId) {
        return set(PointField.TRACK_ID, trackId);
    }

    public PointBuilder sensor(String sensor) {
        return set(PointField.SENSOR, sensor);
    }

    public PointBuilder facility(String facility) {
        return set(PointField.FACILITY, facility);
    }

    public PointBuilder beaconActual(String beaconActual) {
        return set(PointField.BEACON_ACTUAL, beaconActual);
    }

    public PointBuilder beaconAssigned(String beaconAssigned) {
        return set(PointField.BEACON_ASSIGNED, beaconAssigned);
    }

    public PointBuilder flightRules(String flightRules) {
        return set(PointField.FLIGHT_RULES, flightRules);
    }

    public PointBuilder latLong(LatLong latitudeAndLongitude) {
        return set(PointField.LAT_LONG, latitudeAndLongitude);
    }

    public PointBuilder latLong(Double latitude, Double longitude) {
        return set(PointField.LAT_LONG, LatLong.of(latitude, longitude));
    }

    public PointBuilder altitude(Distance altitude) {
        return set(PointField.ALTITUDE, altitude);
    }

    public PointBuilder courseInDegrees(Double courseInDegrees) {
        return set(PointField.COURSE_IN_DEGREES, courseInDegrees);
    }

    public static void checkSpeed(Double speed) {
        if (nonNull(speed)) {
            checkArgument(speed >= 0.0, "Illegal Negative speed: " + speed);
        }
    }

    public PointBuilder speed(Double speed) {
        checkSpeed(speed);
        return set(PointField.SPEED, speed);
    }

    public PointBuilder time(Instant time) {
        return set(PointField.TIME, time);
    }

    /**
     * REPLACE a single field of a Point under construction. This field must already exist.
     *
     * @param field The Field to set
     * @param value The value to set
     *
     * @return This PointBuilder (to allow method chaining)
     */
    private PointBuilder override(PointField field, Object value) {

        checkArgument(field.accepts(value), field + " does not accept this value type");

        if (!data.containsKey(field)) {
            throw new IllegalStateException("The field: " + field + " has not been set");
        }

        if (field.expectedType == String.class && "".equals(value)) {
            throw new IllegalArgumentException(
                "Cannot assign the empty String to the field: " + field
                    + ".  " + "Null is preferred in this case for clarity.");

        }

        data.put(field, value);
        return this;
    }

    public PointBuilder butCallsign(String callsign) {
        return override(PointField.CALLSIGN, callsign);
    }

    public PointBuilder butAircraftType(String aircraftType) {
        return override(PointField.AIRCRAFT_TYPE, aircraftType);
    }

    public PointBuilder butTrackId(String trackId) {
        return override(PointField.TRACK_ID, trackId);
    }

    public PointBuilder butSensor(String sensor) {
        return override(PointField.SENSOR, sensor);
    }

    public PointBuilder butFacility(String facility) {
        return override(PointField.FACILITY, facility);
    }

    public PointBuilder butBeaconActual(String beaconActual) {
        return override(PointField.BEACON_ACTUAL, beaconActual);
    }

    public PointBuilder butBeaconAssigned(String beaconAssigned) {
        return override(PointField.BEACON_ASSIGNED, beaconAssigned);
    }

    public PointBuilder butFlightRules(String flightRules) {
        return override(PointField.FLIGHT_RULES, flightRules);
    }

    public PointBuilder butLatLong(LatLong latitudeAndLongitude) {
        return override(PointField.LAT_LONG, latitudeAndLongitude);
    }

    public PointBuilder butLatLong(Double latitude, Double longitude) {
        return butLatLong(LatLong.of(latitude, longitude));
    }

    public PointBuilder butAltitude(Distance altitude) {
        return override(PointField.ALTITUDE, altitude);
    }

    public PointBuilder butCourseInDegrees(Double courseInDegrees) {
        return override(PointField.COURSE_IN_DEGREES, courseInDegrees);
    }

    public PointBuilder butSpeed(Double speed) {
        checkSpeed(speed);
        return override(PointField.SPEED, speed);
    }

    public PointBuilder butTime(Instant time) {
        return override(PointField.TIME, time);
    }

    public PointBuilder butNo(PointField field) {

        if (!data.containsKey(field)) {
            throw new IllegalStateException("The field: " + field + " has not been set");
        }
        data.remove(field);

        return this;
    }

    public CommonPoint build() {
        return new CommonPoint(data);
    }

    public EphemeralPoint buildMutable() {
        return EphemeralPoint.from(build());
    }

    public Map<PointField, Object> toMap() {
        /*
         * Return a defensive copy. We don't have to worry about the client altering the values with
         * the returned map (which are still in use here) because the "data" map only contains
         * Immutable objects as values.
         */
        return new HashMap(data);
    }

}
