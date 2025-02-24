
package org.mitre.openaria.pointpairing;

import java.time.Duration;
import java.time.Instant;

import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Time;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;

public class Distances {

    /**
     * Estimate the distance, measured in feet, between two Points. This computation can accept
     * Points from to different Instants in time. To support this flexibility the method creates
     * "synthetic" Points that represent the input Points projected forward or backward in time. The
     * "synthetic" points are created at the average time between the two input Points. It is
     * important to emphasize that measuring distance between synthetic points becomes increasingly
     * error-prone as the time between the two input Points increases. This error is caused by (1)
     * the policy of not adjusting altitude measurements when creating synthetic points and (2)
     * magnifying any and all error in an input point's speed and/or course values.
     *
     * @param p1                     A Point
     * @param p2                     A Point
     * @param maxTimeDeltaInMillisec A maximum allowable time difference between these points. This
     *                               parameter is required in order to remind the user that the
     *                               process of measuring from "synthetic" points is numerically
     *                               unstable.
     *
     * @return The number of feet between these Points
     */
    public static double estimateDistanceInFeet(Point p1, Point p2, long maxTimeDeltaInMillisec) {

        //using sythetic Points to estimate physcial distances is numerically unstable
        verifyTimeDeltaIsSmall(p1, p2, maxTimeDeltaInMillisec);

        Instant avgTime = Time.averageTime(p1.time(), p2.time());

        //this metric ignores time and only reflects distance (measured in feet)
        PointDistanceMetric metric = new PointDistanceMetric(0.0, 1.0);

        return metric.distanceBtw(
            projectPointAtNewTime(p1, avgTime),
            projectPointAtNewTime(p2, avgTime)
        );
    }

    /*
     * Using sythetic Points to estimate physcial distances is more and more unreliable as the
     * timespan between the "real points" grows. Therefore, ensure the timeDelta is small.
     */
    private static void verifyTimeDeltaIsSmall(Point p1, Point p2, long maxTimeDeltaInMillisec) {

        Duration timeDelta = Duration.between(p1.time(), p2.time());
        timeDelta = timeDelta.abs();

        if (timeDelta.toMillis() > maxTimeDeltaInMillisec) {
            throw new IllegalArgumentException(
                "Times are too far apart to permit interpolation.  "
                    + "The timeDelta (in millisec) was " + timeDelta.toMillis()
                    + " but the limit was " + maxTimeDeltaInMillisec
            );
        }
    }

    /**
     * Estimate where the input point would be at the newTime given the point course and speed
     * values. This process does not consider (i.e. alter) altitude climb rates when making the
     * projection. The underlying assumption is that altitude should not change much if the newTime
     * is close to the input Point's time. (And this should be the case).
     *
     * @param point   The reference Point. This point is not altered in any way
     * @param newTime The time of the synthetic "projected" point
     *
     * @return A copy of the input Point that is moved forward or backward (along the direction of
     *     travel) depending on if the newTime is earlier or later than the time of the reference
     *     Point.
     */
    static Point projectPointAtNewTime(Point point, Instant newTime) {

        //skip projection when data doesn't support it
        if (point.speed() == null || point.course() == null) {
            return new PointBuilder(point).time(newTime).build();
        }

        Duration timeDelta = Duration.between(point.time(), newTime);

        //can be negative....but that's ok..
        double distanceInNM = distTraveledInNM(point.speed().inKnots(), timeDelta);

        LatLong startPoint = point.latLong();
        LatLong endPoint = startPoint.projectOut(point.course().inDegrees(), distanceInNM);

        return new PointBuilder(point)
            .latLong(endPoint)
            .time(newTime)
            .build();
    }

    /*
     * This method could be made public. If this is done (1) a decision needs to be made if negative
     * durations and speeds are permited and (2) the method should be moved to a more appropriate
     * file.
     */
    private static double distTraveledInNM(double speedInKnots, Duration time) {

        double MILLISEC_PER_HOUR = 1000 * 60 * 60;
        double fractionOfHour = ((double) time.toMillis()) / (MILLISEC_PER_HOUR);
        double distanceInNM = speedInKnots * fractionOfHour;

        return distanceInNM;
    }
}
