
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Double.max;
import static java.lang.Double.min;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Distance.Unit;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.TimeWindow;

import com.google.common.collect.Range;

public class Interpolate {

    /**
     * Use Linear Interpolation to build a new Point between p1 and p2. This method does not permit
     * extrapolation. In other words, the targetTime must be between the close interval bounded by
     * the times of the two input points.
     *
     * @param p1         The earlier of the two input point
     * @param p2         The later of the two input points
     * @param targetTime The time of the interpolated point.
     *
     * @return
     */
    public static <T> Point<T> interpolate(Point<T> p1, Point<T> p2, Instant targetTime) {
        checkNotNull(p1, "Cannot perform interpolation when the first input points is null");
        checkNotNull(p2, "Cannot perform interpolation when the second input points is null");
        checkNotNull(targetTime, "Cannot perform interpolation when the targetTime is null");

        checkArgument(
            p1.time().isBefore(p2.time()) || p1.time().equals(p2.time()),
            "The input points must be in chronological order"
        );

        TimeWindow window = TimeWindow.of(p1.time(), p2.time());

        checkArgument(
            window.contains(targetTime),
            "The targetTime is outside the required time window"
        );

        if (p1.time().equals(targetTime)) {
            return (new PointBuilder(p1)).build();
        } else if (p2.time().equals(targetTime)) {
            return (new PointBuilder(p2)).build();
        } else {

            double fraction = window.toFractionOfRange(targetTime);

            //build an interpolated point
            LatLong interpolatedLatLong = interpolateLatLong(p1.latLong(), p2.latLong(), fraction);

            Double interpolatedCourseInDegrees = interpolateCourse(p1.course(), p2.course(), fraction);

            //correct the interpolated course when one of the input values was null
            if (interpolatedCourseInDegrees == null) {
                interpolatedCourseInDegrees = Spherical.courseInDegrees(p1.latLong(), p2.latLong());
            }

            double interpolatedSpeed = interpolate(
                p1.speedInKnots(),
                p2.speedInKnots(),
                fraction
            );
            Distance interpolatedAltitude = interpolate(
                p1.altitude(),
                p2.altitude(),
                fraction
            );

            //return a copy of the 1st input point but with corrected trajectory data
            return (new PointBuilder(p1))
                .butLatLong(interpolatedLatLong)
                .butCourseInDegrees(interpolatedCourseInDegrees)
                .butSpeed(interpolatedSpeed)
                .butAltitude(interpolatedAltitude)
                .butTime(targetTime)
                .build();
        }
    }

    public static LatLong interpolateLatLong(LatLong p1, LatLong p2, double fraction) {

        double maxLat = max(p1.latitude(), p2.latitude());
        double minLat = min(p1.latitude(), p2.latitude());
        checkArgument(maxLat - minLat <= 90.0, "Interpolation is unsafe at this distance (latitude)");

        double maxLong = max(p1.longitude(), p2.longitude());
        double minLong = min(p1.longitude(), p2.longitude());
        checkArgument(maxLong - minLong <= 180.0, "Interpolation is unsafe at this distance (longitude)");

        return new LatLong(
            interpolate(p1.latitude(), p2.latitude(), fraction),
            interpolate(p1.longitude(), p2.longitude(), fraction)
        );
    }

    private static final Range VALID_COURSE_RANGE = Range.closed(0.0, 360.0);
    private static final Range VALID_FRACTION_RANGE = Range.closed(0.0, 1.0);

    /**
     * Compute a course that occurs some fraction of the way between a starting course and an ending
     * course. Note: this method accepts Object Doubles and not primitive doubles so that it is
     * easier to use with other code (like RH messages) that usually have course data but not
     * always
     *
     * @param c1       The starting course
     * @param c2       The ending course
     * @param fraction The fraction of the way to move from the starting course to the ending course
     *                 (must be between 0 and 1).
     *
     * @return A course in degrees (between 0 and 360) or null if either c1 or c2 is null.
     */
    public static Double interpolateCourse(Double c1, Double c2, double fraction) {

        if (c1 == null || c2 == null) {
            return null;
        }

        checkArgument(VALID_COURSE_RANGE.contains(c1), "The 1st course: " + c1 + " is not in range");
        checkArgument(VALID_COURSE_RANGE.contains(c2), "The 2nd course: " + c2 + " is not in range");
        checkArgument(VALID_FRACTION_RANGE.contains(fraction), "The fraction: " + fraction + " is not in range");

        double angleDelta = Spherical.angleDifference(c2, c1);
        Double course = c1 + interpolate(0.0, angleDelta, fraction);
        return Spherical.mod(course, 360.0d);
    }

    /**
     * Compute a relative position/value on the "line" between the startValue and the endValue.
     *
     * @param startValue The starting point of a linear interpolation. If fraction = 0.0 this will
     *                   be the result.
     * @param endValue   The ending point of a linear interpolation. If fraction = 1.0 this will be
     *                   the result.
     * @param fraction   The fraction of the way between startValue and endValue (not restricted to
     *                   a 0-1 range)
     *
     * @return The interpolated value
     */
    public static double interpolate(double startValue, double endValue, double fraction) {
        double delta = endValue - startValue;
        return startValue + delta * fraction;
    }

    public static Distance interpolate(Distance startValue, Distance endValue, double fraction) {
        Unit unit = startValue.nativeUnit();
        double interpolatedAmount = interpolate(startValue.in(unit), endValue.in(unit), fraction);

        return Distance.of(interpolatedAmount, unit);
    }
}
