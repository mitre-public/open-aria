package org.mitre.openaria.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.mitre.openaria.core.Points.NULLABLE_COMPARATOR;

import java.time.Instant;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;


public record Point<T>(Position position, Velocity velocity, String trackId,
                       T rawData) implements HasPosition, HasTime, Comparable<Point<T>> {

    @Override
    public Instant time() {
        return position().time();
    }

    @Override
    public LatLong latLong() {
        return position().latLong();
    }

    /** @return The altitude of this Position (which may be null). */
    public Distance altitude() {
        return position().hasAltitude() ? position().altitude() : null;
    }


    public Course course() {
        return nonNull(velocity) ? velocity.course() : null;
    }

    public Speed speed() {
        return nonNull(velocity) ? velocity.speed() : null;
    }

    /**
     * This comparison technique generates a strict ordering between two Points using as little data
     * as possible. This method is design to benefit when Point implementations lazily extract
     * infrequently used fields like beacon code and callsign. In other words, this method will
     * usually return a result after accessing only the times and latLongs of the two input Points
     * (which are very likely to have already been parsed)
     *
     * @param other A second Point
     *
     * @return The result of comparing the two Points by their: time, LatLong, altitude, speed,
     *     course, callsign, beaconActual, and trackId (in that order).
     */
    @Override
    public int compareTo(Point other) {

        int timeResult = time().compareTo(other.time());
        if (timeResult != 0) {
            return timeResult;
        }

        int latLongResult = NULLABLE_COMPARATOR.compare(latLong(), other.latLong());
        if (latLongResult != 0) {
            return latLongResult;
        }

        int altitudeResult = NULLABLE_COMPARATOR.compare(altitude(), other.altitude());
        if (altitudeResult != 0) {
            return altitudeResult;
        }

        // @todo -- Prefer to sort by this (just after time)
        int trackIdResult = NULLABLE_COMPARATOR.compare(trackId(), other.trackId());
        return trackIdResult;
    }


    /**
     * Some Point implementations may implement HasAircraftDetails, if so, return callsign info.
     */
    public boolean hasValidCallsign() {
        if (rawData instanceof HasAircraftDetails acDetails) {
            String cs = acDetails.callsign();
            return !(cs == null || cs.equals(""));
        }
        return false;
    }

    /**
     * If the concrete Point implementations also implements HasAircraftDetails then return a
     * boolean telling us if the callsign is available. If the concrete Point implementation does
     * not also implement HasAircraftDetails the throw an {@link UnsupportedOperationException}
     */
    public boolean callsignIsMissing() {
        if (rawData instanceof HasAircraftDetails acDetails) {
            String cs = acDetails.callsign();
            return (cs == null || cs.equals(""));
        }
        throw new UnsupportedOperationException("This Point implementation does not support callsign");
    }

    public boolean altitudeIsMissing() {
        return isNull(altitude());
    }

    public boolean hasTrackId() {
        return !trackIdIsMissing();
    }

    public boolean trackIdIsMissing() {
        return isNull(trackId) || trackId.equals("");
    }

    /**
     * @return A PointBuilder that can be used to manually assemble Points. This is particularly
     *     useful for unit testing (because most Points are generated by parsing raw data like NOP
     *     or ASDE).
     */
    public static <T> PointBuilder<T> builder() {
        return new PointBuilder<>();
    }

    /**
     * @param point Seed the PointBuilder with information from this Point
     *
     * @return A PointBuilder that can be used to manually assemble Points. This is particularly
     *     useful for unit testing (because most Points are generated by parsing raw data like NOP
     *     or ASDE).
     */
    public static <T> PointBuilder<T> builder(Point<T> point) {
        return new PointBuilder<>(point);
    }
}
