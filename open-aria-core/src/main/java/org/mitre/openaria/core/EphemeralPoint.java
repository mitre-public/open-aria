

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;
import static org.mitre.openaria.core.PointBuilder.checkSpeed;
import static org.mitre.openaria.core.PointField.*;
import static org.mitre.openaria.core.Points.toMap;
import static org.mitre.openaria.core.temp.Extras.HasSourceDetails;
import static org.mitre.openaria.core.temp.Extras.SourceDetails;

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
public class EphemeralPoint<T> implements MutablePoint<T>, HasSourceDetails{

    private final HashMap<PointField, Object> data;

    private SourceDetails sourceDetails;

    private T rawData;

    public static <T> EphemeralPoint<T> from(Point<T> sourcePoint, SourceDetails sourceDetails) {
        return new EphemeralPoint<T>(sourcePoint, sourceDetails);
    }

    public static <T> EphemeralPoint<T> from(Point<T> sourcePoint) {
        if(sourcePoint instanceof HasSourceDetails) {
            SourceDetails sd = ((HasSourceDetails) sourcePoint).sourceDetails();
            return new EphemeralPoint<T>(sourcePoint, sd);
        }

        return new EphemeralPoint<>(sourcePoint, null);
    }

    private EphemeralPoint(Point<T> sourcePoint, SourceDetails sd) {
        this.data = newHashMap(toMap(sourcePoint));
        this.sourceDetails = sd;
        this.rawData = sourcePoint.rawData();
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

    public void set(SourceDetails sd) {
        this.sourceDetails = sd;
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
    public SourceDetails sourceDetails() {
        return sourceDetails;
    }

    @Override
    public T rawData() {
        return null;
    }
}
