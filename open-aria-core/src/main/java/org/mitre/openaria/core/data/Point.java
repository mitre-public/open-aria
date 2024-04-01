package org.mitre.openaria.core.data;

import java.time.Instant;
import java.util.Comparator;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Position;
import org.mitre.openaria.core.Track;

/**
 * A Point is a single piece of time-stamped location data. A Point describes one object at one
 * moment in time. Consequently, you typically work with Points in Collections where the collection
 * of points describes an object's movement path over time see: {@link Track}.
 * <p>
 * This interface is generic because location data comes in many different formats and flavors. This
 * interface only covers the shared "time and location" aspects of these various formats. Features
 * that are unlikely to be support by ALL data formats are intentionally excluded from this
 * interface. Consequently, this interface will frequently be decorated or extended to add support
 * for format specific features.
 */
public interface Point<T> extends HasPosition, HasTime, Comparable<Point> {

    /*
     * The intentional decision to exclude format specific features was made to (1) keep this
     * interface simple by limiting its scope and (2) ensure the design & architecture for
     * supporting format specific features is encouraged to mature into a powerful and useful
     * capability on its own right
     */

    /**
     * Provide access, when feasible, to any raw data from which this Point was derived.
     * <p>
     * This method is in this interface to enable (and strongly encourage) a design where the raw
     * byte[], String, or object that define a Point is immutably stored. This design is likely to
     * simplify decorating classes because those format-specific implementations will have easy
     * access to the raw data.
     * <p>
     * An implicit assumption here is that raw data will often be in a byte-dense format so leaving
     * the data in its native format will frequently be an attractive option.
     *
     * @return The raw data from which this Point was derived. For example, provide the original
     *     byte[] or String from which this point was parsed. Be Advised: this method may not be
     *     supported if the Point is not directly backed by raw data (i.e., the Point was generated
     *     programmatically)
     */
    T rawData();

    /**
     * @return A String that identifies a particular entity in a stream of position data (e.g., a
     *     specific aircraft, vehicle, or person). This id is used to link multiple Points
     *     describing the same entity together across time (e.g. form movement paths). It is also
     *     used to distinguish distinct entities within a datafeed.
     */
    String linkId();

    Instant time();

    LatLong latLong();

    Distance altitude();

    /**
     * @return A String that represents this point as if it were defined via the default CSV data
     *     format.  Note: this default implementation only harvests fields accessible by the Point
     *     interface. Fields added via decoration or composition will be lost.
     *     <p>
     *     When possible, this method should be overridden to ensure all fields are represented in
     *     the output (not just the fields available in this interface)
     */
    default String asCsvText() {
        String lat = String.format("%.4f", latitude());
        String lng = String.format("%.4f", longitude());
        String alt = String.format("%.0f", altitude().inFeet());

        return ",," + time().toString() + "," + this.linkId() + "," + lat + "," + lng + "," + alt;
    }

    static String asCsvText(Instant time, String linkId, LatLong latLong, Distance altitude) {

        String lat = String.format("%.4f", latLong.latitude());
        String lng = String.format("%.4f", latLong.longitude());
        String alt = String.format("%.0f", altitude.inFeet());

        return ",," + time.toString() + "," + linkId + "," + lat + "," + lng + "," + alt;
    }

//    static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
//
//    default String asCsvText(Function<T, byte[]> serializer) {
//        byte[] payLoadAsBytes = serializer.apply(rawData());
//        String base64Encoding = BASE64_ENCODER.encodeToString(payLoadAsBytes);
//
//        return ",," + time().toString() + "," + this.linkId() + "," + this.latitude() + "," + this.longitude() + "," + this.altitude().inFeet() + "," + base64Encoding;
//    }

    static <T> PointRecord.Builder<T> builder() {
        return new PointRecord.Builder<>();
    }

    /**
     * This comparison technique generates a strict ordering between two Points using as little data
     * as possible.
     *
     * @param other A second Point
     *
     * @return The result of comparing the two Points by their: time, LatLong, altitude, and linkId
     *     (in that order).
     */
    @Override
    default int compareTo(Point other) {

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

        int trackIdResult = NULLABLE_COMPARATOR.compare(linkId(), other.linkId());
        if (trackIdResult != 0) {
            return trackIdResult;
        }

        return 0;
    }

//    default boolean altitudeIsMissing() {
//        return altitude() == null;
//    }
//
//    default boolean hasTrackId() {
//        return !trackIdIsMissing();
//    }
//
//    default boolean trackIdIsMissing() {
//        String trackId = trackId();
//        return (trackId == null || trackId.equals(""));
//    }


    default Position position() {
        return new Position(time(), latLong(), altitude());
    }


    /**
     * This Comparator should only be used to help the implementation of Point.compareTo handle
     * Points with missing/null values. This Comparator does not properly handle cases where the
     * inputs are of different classes that should not be compared against one another.
     */
    Comparator<Comparable> NULLABLE_COMPARATOR = (o1, o2) -> {

        if (o1 != null && o2 != null) {
            //when both are not null return the comparision
            return o1.compareTo(o2);
        } else if (o1 == null && o2 == null) {
            //when both are null return 0
            return 0;
        } else if (o1 == null) {
            //when left is null return "right is greater"
            return -1;
        } else {
            //when right is null return "left is greater"
            return 1;
        }
    };
}
