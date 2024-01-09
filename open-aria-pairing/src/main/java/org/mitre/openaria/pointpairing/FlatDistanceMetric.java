
package org.mitre.openaria.pointpairing;

import java.time.Duration;

import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.collect.DistanceMetric;

/**
 * This DistanceMetric measures "a" distance between two Points. The distance computed is a function
 * of both physical distance (measure in feet) AND temporal distance (measured in milliseconds).
 * <p>
 * A PointDistanceMetric returns: (timeCoef * timeDeltaInMillis) + (distCoef * distDeltaInFt).
 * <p>
 * Importantly, input Points are not required to be from the same moment in time. Therefore, this
 * distance metric can be used to search through any and all Points to identify Points that are
 * "close by".
 * <p>
 * A DistanceMetric can be used to find Points that are "close enough" to warrant more sophisticated
 * analysis (that usually requires the input Point to be from the same moment in Time). For example,
 * if you want to determine if two aircraft are within 500ft you would not use this distance metric
 * in isolation. This DistanceMetric cannot be relied on to compute the "500 ft" determination by
 * itself because it does not enforce the input Points to be from the same instant in Time. However,
 * this Metric can be used to quickly identify Pairs of Points that are simultaneously both close in
 * physical space and close in time. This is clearly the best place to begin trying to find Points
 * that are truly within 500 ft at one exact moment in time.
 * <p>
 * This class does NOT use altitude information when computing the "distDeltaInFt" value. This
 * distance computation only relies on Lat/Long data.
 */
public class FlatDistanceMetric implements DistanceMetric<Point> {

    private static final long serialVersionUID = 578013786897548608L;

    private final double timeCoef;

    private final double distanceCoef;

    private long numCalls;

    /**
     * Create a new FlatDistanceMetric that returns: (timeCoef * timeDeltaInMillis) + (distanceCoef
     * * distDeltaInFeet)
     *
     * @param timeCoef     A constant that weights the millisecond time span between Points
     * @param distanceCoef A constant that weights the distance (in feet) between Points (ignoring
     *                     all altitude difference).
     */
    public FlatDistanceMetric(double timeCoef, double distanceCoef) {
        this.timeCoef = timeCoef;
        this.distanceCoef = distanceCoef;
        this.numCalls = 0L;
    }

    public FlatDistanceMetric() {
        this(1.0, 1.0);
    }

    public double timeCoef() {
        return timeCoef;
    }

    public double distanceCoef() {
        return distanceCoef;
    }

    /**
     * @param p1 A Point with latitude, longitude, and time
     * @param p2 A Point with latitude, longitude, and time
     *
     * @return A distance measurement between these points. The distance measurement is a metric in
     *     the formal algebraic sense. Consequently, this metric can be use to help organize point
     *     data. The metric measurement returned is the sum of the difference in the time dimension
     *     (measured in milliseconds) and the difference in the "physical space" dimension (measured
     *     in ft). Note: Altitude is ignored
     */
    @Override
    public double distanceBtw(Point p1, Point p2) {
        /*
         * Note: We intentionally do not check these inputs for NPEs or missing values.
         *
         * Performing these checks has a significant performance impact because this method is
         * typically called extremely often. This real cost is not justified given the frequency in
         * which lat, long, or time data will be missing.
         */

        numCalls++;

        Duration timeDelta = Duration.between(p1.time(), p2.time()); //can be positive of negative
        timeDelta = timeDelta.abs();

        p1.distanceInNmTo(p2);
        Double horizontalDistanceInNm = p1.distanceInNmTo(p2);

        Double horizontalDistanceInFeet = horizontalDistanceInNm * Spherical.feetPerNM();

        return (distanceCoef * horizontalDistanceInFeet) + (timeCoef * timeDelta.toMillis());
    }

    public long numCalls() {
        return this.numCalls;
    }
}
