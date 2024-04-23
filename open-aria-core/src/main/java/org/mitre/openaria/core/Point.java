

package org.mitre.openaria.core;

import static org.mitre.openaria.core.Points.NULLABLE_COMPARATOR;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.Position;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;

/**
 * A Point object is a single piece of location data. Since we want to support multiple types of
 * location data this interface is meant to be decorated.
 *
 * @param <T>
 */
public interface Point<T> extends HasPosition, HasTime, Comparable<Point> {

    T rawData();

    String trackId();  //almost always an Integer, but sometime this is a number and letter like "25F"

    String beaconActual();

    String beaconAssigned();

    Distance altitude();

    Double course();

    Double speedInKnots();


    /**
     * @return A String that represent this point as if it were a raw NOP Radar Hit (RH) Message.
     *     When possible, this method will delegate to the implementing Point class (which may
     *     contain the raw NOP Message). The default implementation "harvests" the data fields
     *     available as part of the Point interface and builds a "faux RH Message" from that data.
     */
     default String asNop() {

        /*
         * If the implementing class cannot provide the raw Nop RH Message itself encode whatever
         * data is available via the Point interface itself.
         */
        return (new NopEncoder()).asRawNop(this);
    }

//    /** @return a KeyExtractor that generates String keys by concatenating trackId() and facility() */
//    static KeyExtractor<Point> keyExtractor() {
//        return (Point p) -> p.trackId() + p.facility();
//    }

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
    public default int compareTo(Point other) {

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

        int speedResult = NULLABLE_COMPARATOR.compare(speedInKnots(), other.speedInKnots());
        if (speedResult != 0) {
            return speedResult;
        }

        int courseResult = NULLABLE_COMPARATOR.compare(course(), other.course());
        if (courseResult != 0) {
            return courseResult;
        }

        int beaconResult = NULLABLE_COMPARATOR.compare(beaconActual(), other.beaconActual());
        if (beaconResult != 0) {
            return beaconResult;
        }

        int trackIdResult = NULLABLE_COMPARATOR.compare(trackId(), other.trackId());
        if (trackIdResult != 0) {
            return trackIdResult;
        }

        return 0;
    }


    /**
     * Some Point implementations may implement HasAircraftDetails, if so, return callsign
     * info.
     */
    default boolean hasValidCallsign() {
        if (this instanceof HasAircraftDetails acDetails) {
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
    default boolean callsignIsMissing() {
        if (this instanceof HasAircraftDetails acDetails) {
            String cs = acDetails.callsign();
            return (cs == null || cs.equals(""));
        }
        throw new UnsupportedOperationException("This Point implementation does not support callsign");
    }

    public default boolean altitudeIsMissing() {
        return altitude() == null;
    }

//    public default boolean hasFlightRules() {
//        return !flightRulesIsMissing();
//    }


//    public default boolean flightRulesIsMissing() {
//        String rules = this.flightRules();
//        return (rules == null || rules.equals(""));
//    }

    public default boolean hasTrackId() {
        return !trackIdIsMissing();
    }

    public default boolean trackIdIsMissing() {
        String trackId = trackId();
        return (trackId == null || trackId.equals(""));
    }

    public default boolean hasValidBeaconActual() {
        return !beaconActualIsMissing();
    }

    public default boolean beaconActualIsMissing() {
        String beacon = beaconActual();
        return (beacon == null || beacon.equals("") || beacon.equals("null"));
    }

    public default int beaconActualAsInt() {
        return Integer.parseInt(beaconActual());
    }

    /**
     * @return A PointBuilder that can be used to manually assemble Points. This is particularly
     *     useful for unit testing (because most Points are generated by parsing raw data like NOP
     *     or ASDE).
     */
    public static PointBuilder builder() {
        return new PointBuilder();
    }

    /**
     * @param point Seed the PointBuilder with information from this Point
     *
     * @return A PointBuilder that can be used to manually assemble Points. This is particularly
     *     useful for unit testing (because most Points are generated by parsing raw data like NOP
     *     or ASDE).
     */
    public static PointBuilder builder(Point point) {
        return new PointBuilder(point);
    }

    default Position position() {
        return Position.builder()
            .latLong(this.latLong())
            .time(this.time())
            .altitude(this.altitude())
            .build();
    }
}
