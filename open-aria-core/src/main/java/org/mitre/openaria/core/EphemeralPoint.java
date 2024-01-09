

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static org.mitre.openaria.core.PointBuilder.checkSpeed;
import static org.mitre.openaria.core.PointField.AIRCRAFT_TYPE;
import static org.mitre.openaria.core.PointField.ALONG_TRACK_DISTANCE;
import static org.mitre.openaria.core.PointField.ALTITUDE;
import static org.mitre.openaria.core.PointField.BEACON_ACTUAL;
import static org.mitre.openaria.core.PointField.BEACON_ASSIGNED;
import static org.mitre.openaria.core.PointField.CALLSIGN;
import static org.mitre.openaria.core.PointField.COURSE_IN_DEGREES;
import static org.mitre.openaria.core.PointField.CURVATURE;
import static org.mitre.openaria.core.PointField.FACILITY;
import static org.mitre.openaria.core.PointField.FLIGHT_RULES;
import static org.mitre.openaria.core.PointField.LAT_LONG;
import static org.mitre.openaria.core.PointField.SENSOR;
import static org.mitre.openaria.core.PointField.SPEED;
import static org.mitre.openaria.core.PointField.TIME;
import static org.mitre.openaria.core.PointField.TRACK_ID;
import static org.mitre.openaria.core.Points.toMap;

import java.time.Instant;
import java.util.HashMap;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * An EphemeralPoint is a MutablePoint that is meant to be <b>repeatedly</b> manipulated.
 * EphemeralPoints should be used with caution because mutable data can leads to bugs. Despite this
 * risk, the EphemeralPoint can be useful when Point data is likely to be repeatedly altered (like
 * during Track smoothing). In this case, repeatedly copying data from Immutable Point to Immutable
 * Point can become costly.
 */
public class EphemeralPoint implements MutablePoint {

    private final HashMap<PointField, Object> data;

    public static EphemeralPoint from(Point sourcePoint) {
        return new EphemeralPoint(sourcePoint);
    }

    private EphemeralPoint(Point sourcePoint) {
        this.data = newHashMap(toMap(sourcePoint));
    }

    /**
     * Set a single field of a Point under construction.
     *
     * @param field The Field to set
     * @param value The value to set
     */
    public void set(PointField field, Object value) {

        checkArgument(field.accepts(value), field + " does not accept this value type");

        if (field.expectedType == String.class) {
            if ("".equals(value)) {
                throw new IllegalArgumentException(
                    "Cannot assign the empty String to the field: " + field + ".  "
                        + "Null is prefered in this case for clarity.");
            }
        }

        data.put(field, value);

        if (field == SPEED) {
            checkSpeed((Double) value);
        }
    }

    @Override
    public String callsign() {
        return (String) data.get(CALLSIGN);
    }

    @Override
    public String aircraftType() {
        return (String) data.get(AIRCRAFT_TYPE);
    }

    @Override
    public String trackId() {
        return (String) data.get(TRACK_ID);
    }

    @Override
    public String sensor() {
        return (String) data.get(SENSOR);
    }

    @Override
    public String facility() {
        return (String) data.get(FACILITY);
    }

    @Override
    public String beaconActual() {
        return (String) data.get(BEACON_ACTUAL);
    }

    @Override
    public String beaconAssigned() {
        return (String) data.get(BEACON_ASSIGNED);
    }

    @Override
    public String flightRules() {
        return (String) data.get(FLIGHT_RULES);
    }

    @Override
    public LatLong latLong() {
        return (LatLong) data.get(LAT_LONG);
    }

    @Override
    public Distance altitude() {
        return (Distance) data.get(ALTITUDE);
    }

    @Override
    public Double course() {
        return (Double) data.get(COURSE_IN_DEGREES);
    }

    @Override
    public Double speedInKnots() {
        return (Double) data.get(SPEED);
    }

    @Override
    public Instant time() {
        return (Instant) data.get(TIME);
    }

    @Override
    public Double curvature() {
        return (Double) data.get(CURVATURE);
    }

    @Override
    public Double alongTrackDistance() {
        return (Double) data.get(ALONG_TRACK_DISTANCE);
    }
}
