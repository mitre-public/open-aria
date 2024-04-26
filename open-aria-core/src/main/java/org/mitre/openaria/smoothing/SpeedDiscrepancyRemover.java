
package org.mitre.openaria.smoothing;

import java.time.Duration;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;

public class SpeedDiscrepancyRemover implements DataCleaner<MutableTrack> {

    private static final Double SPEED_DISCREPANCY_OFFSET = 6.0;
    private static final Double SPEED_DISCREPANCY_SLOPE = 1.5;

    /**
     * Create a cleaned version of the track with speed discrepancy points removed.  A point has a
     * speed discrepancy if there is a significant difference between its raw speed and the average
     * speed computed from its position and the positions of its neighbors.
     *
     * @param track A Track
     *
     * @return An Optional Track with speed discrepancies removed.
     */
    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        Collection<Point> speedDiscrepancies = findSpeedDiscrepancies(track);

        track.points().removeAll(speedDiscrepancies);

        return (track.points().isEmpty())
            ? Optional.empty()
            : Optional.of(track);
    }

    private Collection<Point> findSpeedDiscrepancies(MutableTrack track) {

        NavigableSet<Point> outliers = new TreeSet<>();

        Point point1 = null;
        Point point2 = null;
        Point point3 = null;

        for (Point point : track.points()) {
            point1 = point2;
            point2 = point3;
            point3 = point;

            if (point1 == null || point2 == null) {
                continue;
            }

            if (midPointHasSpeedDiscrepancy(point1, point2, point3)) {
                outliers.add(point2);
            }
        }
        return outliers;
    }

    private boolean midPointHasSpeedDiscrepancy(Point p1, Point p2, Point p3) {

        double rawSpeed = p2.speedInKnots();
        double averageSpeed = speedBetween(p1, p2, p3).inKnots();
        return isDiscrepancyBetween(rawSpeed, averageSpeed);
    }

    /**
     * Determine if there is a significant discrepancy between two speeds. SPEED_DISCREPANCY_OFFSET
     * is the minimum speed which would create a discrepancy if the other speed were 0.
     * SPEED_DISCREPANCY_SLOPE is the minimum ratio between the speeds needed to create a
     * discrepancy.  This method is symmetric, so the order of the speeds does not matter.
     *
     * @param speed1 First test speed in knots
     * @param speed2 Second test speed in knots
     *
     * @return Whether or not there is a significant discrepancy between the two speeds.
     */
    private boolean isDiscrepancyBetween(double speed1, double speed2) {

        return (speed1 > SPEED_DISCREPANCY_OFFSET + SPEED_DISCREPANCY_SLOPE * speed2) ||
            (speed2 > SPEED_DISCREPANCY_OFFSET + SPEED_DISCREPANCY_SLOPE * speed1);
    }

    private Speed speedBetween(Point p1, Point p2, Point p3) {

        Distance distance = Distance.ofNauticalMiles(p1.distanceInNmTo(p2) + p2.distanceInNmTo(p3));
        Duration time = Duration.between(p1.time(), p3.time());
        return distance.dividedBy(time);
    }
}
