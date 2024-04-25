package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;
import static org.mitre.caasd.commons.Spherical.courseInDegrees;

import java.time.Duration;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.math.Vector;

/**
 * PointPair wraps two Points to provide a cleaner API for common tasks like computing separation
 * values.
 */
public class PointPair {

    private final Point point1;
    private final Point point2;

    public PointPair(Point point1, Point point2) {
        this.point1 = checkNotNull(point1);
        this.point2 = checkNotNull(point2);
    }

    public static PointPair of(Point point1, Point point2) {
        return new PointPair(point1, point2);
    }

    public Point point1() {
        return point1;
    }

    public Point point2() {
        return point2;
    }

    public Distance altitudeDelta() {
        double delta = abs(point1.altitude().inFeet() - point2.altitude().inFeet());
        return Distance.of(delta, Distance.Unit.FEET);
    }

    public Distance lateralDistance() {
        return point1.latLong().distanceTo(point2.latLong());
    }

    public Speed speedDelta() {
        double delta = abs(point1.speedInKnots() - point2.speedInKnots());
        return Speed.of(delta, KNOTS);
    }

    public Speed magnitudeOfVelocityDelta() {
        Vector velocityDelta = velocityInKnots(point1).minus(velocityInKnots(point2));
        return Speed.of(velocityDelta.magnitude(), KNOTS);
    }

    /**
     * Return the closure rate (in the horizontal plane) between the points. A positive speed
     * indicates that the points are converging, negative means diverging. If the points are within
     * about 6 ft (or less), it is undetermined, so a zero speed is returned.
     *
     * <pre>
     * Projected separation, d(t):
     * d(t) = | (p1 + v1*t) - (p2 + v2*t) |
     *
     * After some algebra/diff eq., the derivative evaluated at zero, is:
     * d'(0) = ( dp * dv ) / | dp |
     *
     * where dp = p1 - p2, and dv = v1 - v2.
     * </pre>
     */
    public Speed horizontalClosure() {

        Vector dp = vectorBetweenPointsInNm();
        double dpMagnitude = dp.magnitude();

        if (dpMagnitude < 0.001) {
            return Speed.of(0.0, KNOTS);
        } else {
            Vector dv = velocityInKnots(point1).minus(velocityInKnots(point2));
            return Speed.of(dp.dot(dv) / dpMagnitude, KNOTS);
        }
    }

    public double courseDelta() {
        return Spherical.angleDifference(point1.course(), point2.course());
    }

    public Course angleDelta() {
        return Course.ofDegrees(courseDelta());
    }

    /**
     * @param altitudeProximityReq The maximum qualifying altitude separation between the points.
     * @param lateralProximityReq  The maximum qualifying lateral separation between the points.
     *
     * @return True if these points are within both the altitude and lateral distance requirement.
     */
    public boolean areWithin(Distance altitudeProximityReq, Distance lateralProximityReq) {
        //test the altitudeDelta first to reduce the number of calls to lateralDistance()
        return altitudeDelta().isLessThanOrEqualTo(altitudeProximityReq)
            && lateralDistance().isLessThanOrEqualTo(lateralProximityReq);
    }

    public LatLong avgLatLong() {
        return LatLong.quickAvgLatLong(point1.latLong(), point2.latLong());
    }

    public Distance avgAltitude() {
        double avgInFeet = (point1.altitude().inFeet() + point2.altitude().inFeet()) / 2.0;
        return Distance.ofFeet(avgInFeet);
    }

    /**
     * This method assumes the two points are at the same time. The method projects the linear path
     * forward and finds the time at which the distance between the points is minimized, and then
     * returns that time and the minimum distance.
     *
     * <p>This method does not account for curvature of the Earth.
     *
     * @return The time until the cpa and the distance between the aircraft at the cpa
     */
    public ClosestPointOfApproach closestPointOfApproach() {
        checkState(
            point1.time().equals(point2.time()),
            "This function can only be applied to points at the same Instant"
        );

        Vector initialVectorBetweenPoints = vectorBetweenPointsInNm();
        Vector velocityDifference = velocityInKnots(point2).minus(velocityInKnots(point1));

        //will be negative if aircraft are diverging
        double timeOfMinDistanceInHours = timeToProjectedMinDistanceInHours(
            initialVectorBetweenPoints,
            velocityDifference
        );

        if (!Double.isFinite(timeOfMinDistanceInHours) || timeOfMinDistanceInHours <= 0) {
            return new ClosestPointOfApproach(Duration.ZERO, lateralDistance());
        }

        long timeOfMinDistanceInMillis = (long) (timeOfMinDistanceInHours * 1000 * 60 * 60);

        Vector vectorBetweenPointsAtCpa = initialVectorBetweenPoints.plus(
            velocityDifference.times(timeOfMinDistanceInHours)
        );

        return new ClosestPointOfApproach(
            Duration.ofMillis(timeOfMinDistanceInMillis),
            Distance.ofNauticalMiles(vectorBetweenPointsAtCpa.magnitude())
        );
    }

    private double timeToProjectedMinDistanceInHours(Vector vectorBetweenPointsInNm, Vector velocityDifferenceInKnots) {
        return -1 * vectorBetweenPointsInNm.dot(velocityDifferenceInKnots)
            / velocityDifferenceInKnots.dot(velocityDifferenceInKnots);
    }

    private Vector vectorBetweenPointsInNm() {
        double courseBetweenPoints = courseInDegrees(point1.latLong(), point2.latLong());
        double distanceBetweenPoints = lateralDistance().inNauticalMiles();

        return new Vector(
            distanceBetweenPoints * cos(toRadians(courseBetweenPoints)),
            distanceBetweenPoints * sin(toRadians(courseBetweenPoints))
        );
    }

    private Vector velocityInKnots(Point point) {
        requireNonNull(point.speedInKnots(), "Speed cannot be null");
        requireNonNull(point.course(), "Course cannot be null");
        return new Vector(
            point.speedInKnots() * cos(toRadians(point.course())),
            point.speedInKnots() * sin(toRadians(point.course()))
        );
    }
}
