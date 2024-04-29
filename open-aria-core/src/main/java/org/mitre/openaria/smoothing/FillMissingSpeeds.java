
package org.mitre.openaria.smoothing;

import static java.util.Objects.nonNull;
import static org.mitre.openaria.core.Points.speedBetween;

import java.util.ArrayList;
import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import com.google.common.math.Stats;

/**
 * This DataCleaner adds Speed data to Points in a MutableTrack that do not have Speed data.
 * <p>
 * The new Speed values are estimated from the position and time data each points immediate
 * neighbors.
 */
public class FillMissingSpeeds implements DataCleaner<Track> {

    HasNullSpeed predicate = new HasNullSpeed();

    @Override
    public Optional<Track> clean(Track track) {

        if (track.size() < 2) {
            return Optional.empty();
        }

        ArrayList<Point> ptList = new ArrayList<>(track.points());

        // Find sequences of three consecutive points, compute the middle speed when it is missing
        for (int i = 0; i < ptList.size(); i++) {
            Point cur = ptList.get(i);

            if (nonNull(cur) && predicate.test(cur)) {
                Point left = i >= 1 ? ptList.get(i - 1) : null;
                Point right = i < ptList.size() - 1 ? ptList.get(i + 1) : null;

                Point fixed = setMissingSpeed(left, cur, right);
                ptList.set(i, fixed);
            }
        }

        return Optional.of(Track.of(ptList));
    }

    private Point setMissingSpeed(Point pointA, Point pointB, Point pointC) {

        double deducedSpeedInKnots;

        if (pointA == null && pointC != null) {
            //deduce a speed referencing only the separation between pointB and pointC
            deducedSpeedInKnots = speedBetween(pointB, pointC).inKnots();
        } else if (pointA != null && pointC == null) {
            //deduce a speed referencing only the separation between pointA and pointB
            deducedSpeedInKnots = speedBetween(pointA, pointB).inKnots();
        } else if (pointA != null && pointC != null) {
            //deduce a speed referencing all three points: A, B, and C
            //Note: this presumes that all 3 points are roughly in a line (ABC is more like a line, not a triangle)
            double speed1 = speedBetween(pointA, pointB).inKnots(); //a to b
            double speed2 = speedBetween(pointB, pointC).inKnots();  //b to c
            double speed3 = speedBetween(pointA, pointC).inKnots(); //a to c
            deducedSpeedInKnots =  Stats.meanOf(speed1, speed2, speed3);
        } else {
            throw new AssertionError("Cannot deduce a Speed when no other points are known");
        }

        return Point.builder(pointB).butSpeed(deducedSpeedInKnots).build();
    }

}
