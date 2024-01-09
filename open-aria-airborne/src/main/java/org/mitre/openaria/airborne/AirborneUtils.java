

package org.mitre.openaria.airborne;

import static java.util.stream.Collectors.toList;
import static org.mitre.caasd.commons.Distance.max;
import static org.mitre.caasd.commons.Distance.min;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NavigableSet;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.TimeWindow;

public class AirborneUtils {

    /**
     * Determine if a Track is "already established" at a specific altitude. This verifies that all
     * track points in a specific time window -- that ENDS at the query time -- have altitudes
     * within a 100ft range. This means an aircraft takes a few moments of level flight to become
     * established at an altitude.
     *
     * <p>The implementation requires there to be at least two points in the query's time window.
     * Without this requirement a track the never has the same altitude twice can be "established"
     * at an altitude.
     *
     * @param track     The source track
     * @param queryTime A time that the Track could have been established at an altitude
     * @param duration  The Duration of level flight that is required.
     *
     * @return True if the track has two or more points in the query TimeWindow AND all those points
     *     have altitudes within a 100ft range.
     */
    public static boolean isEstablishedAtAltitude(Track track, Instant queryTime, Duration duration) {
        TimeWindow window = TimeWindow.of(queryTime.minus(duration), queryTime);
        NavigableSet<Point> recentPoints = (NavigableSet<Point>) track.subset(window);

        if (recentPoints.isEmpty()) {
            return false;
        }

        List<Distance> recentDistances = recentPoints
            .stream()
            .map(Point::altitude)
            .collect(toList());

        Distance min = min(recentDistances);
        Distance max = max(recentDistances);
        Distance altitudeRange = max.minus(min);

        boolean allAltitudesWithin100Feet = altitudeRange.isLessThanOrEqualTo(Distance.ofFeet(100));
        boolean atLeastTwoPoints = recentPoints.size() >= 2;

        return atLeastTwoPoints && allAltitudesWithin100Feet;
    }
}
