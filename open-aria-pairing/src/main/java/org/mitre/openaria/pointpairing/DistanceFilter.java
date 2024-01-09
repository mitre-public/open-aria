
package org.mitre.openaria.pointpairing;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.Pair;

/**
 * A DistanceFilter will accept or reject Pairs of Points.
 * <p>
 * The goal of a DistanceFilter is to permit judgments based on distance to be made on Points with
 * SLIGHTLY different times (i.e. 15 seconds or less).
 * <p>
 * Points are acceptable if (A) they are close in time and (B) close in physical space (at the
 * average time). The reason points must be close in time is that the underlying computation is
 * susceptible to slight perturbations in the inputs. Increasing the allowable time delta magnifies
 * the potential impact of this error. See TrueDistances.estimateDistanceInFeet(Point, Point,long)
 * for more info.
 */
public class DistanceFilter implements Predicate<Pair<Point, Point>>, Serializable {

    private static final long serialVersionUID = 1L;

    private final double MAX_DISTANCE_IN_FEET;

    private final long MAX_TIME_DELTA_IN_MILLISECS;

    DistanceFilter(double maximumDistanceInFeet, long maxPointTimeDifference) {
        this.MAX_DISTANCE_IN_FEET = maximumDistanceInFeet;
        this.MAX_TIME_DELTA_IN_MILLISECS = maxPointTimeDifference;
    }

    @Override
    public boolean test(Pair<Point, Point> pair) {

        if (timeDeltaIsSmall(pair.first().time(), pair.second().time())) {
            return distIsSmall(pair);
        } else {
            /*
             * reject points with large time deltas because we don't want to rely on a numerically
             * unstable process
             */
            return false;
        }
    }

    private boolean distIsSmall(Pair<Point, Point> pair) {

        double distanceEstimate = Distances.estimateDistanceInFeet(
            pair.first(),
            pair.second(),
            MAX_TIME_DELTA_IN_MILLISECS
        );

        return distanceEstimate <= MAX_DISTANCE_IN_FEET;
    }

    private boolean timeDeltaIsSmall(Instant time, Instant time0) {
        Duration d = Duration.between(time, time0);
        d = d.abs();

        return d.toMillis() <= MAX_TIME_DELTA_IN_MILLISECS;
    }
}
