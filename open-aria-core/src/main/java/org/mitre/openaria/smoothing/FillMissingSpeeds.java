
package org.mitre.openaria.smoothing;

import static java.util.Objects.nonNull;
import static org.mitre.openaria.core.PointField.SPEED;
import static org.mitre.openaria.core.Points.speedBetween;

import java.util.Optional;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.DataCleaner;

import com.google.common.math.Stats;

/**
 * This DataCleaner adds Speed data to Points in a MutableTrack that do not have Speed data.
 * <p>
 * The new Speed values are estimated from the position and time data each points immediate
 * neighbors.
 */
public class FillMissingSpeeds implements DataCleaner<MutableTrack> {

    HasNullSpeed predicate = new HasNullSpeed();

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        if (track.size() < 2) {
            return Optional.empty();
        }

        /*
         * Find sequences of three consecutive points, compute the middle speed when it is missing
         */
        MutablePoint first = null;
        MutablePoint second = null;
        MutablePoint third = null;

        for (MutablePoint point : track.points()) {
            first = second;
            second = third;
            third = point;

            if (nonNull(second) && predicate.test(second)) {
                setMissingSpeed(first, second, third);
            }
        }

        //check the very last point in the track
        if (nonNull(third) && predicate.test(third)) {
            setMissingSpeed(second, third, null);
        }

        return Optional.of(MutableTrack.of(track.points()));
    }

    private void setMissingSpeed(Point pointA, MutablePoint pointB, Point pointC) {

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
        pointB.set(SPEED, deducedSpeedInKnots);
    }

}
