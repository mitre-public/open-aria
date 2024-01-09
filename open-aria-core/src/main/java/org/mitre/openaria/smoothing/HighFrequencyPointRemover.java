
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Sets.newTreeSet;

import java.time.Duration;
import java.time.Instant;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;

/**
 * A HighFrequencyPointRemover removes all Points with time values that are so close together that
 * at least one of the points must be flawed. For example, if normal radar returns occur every 6
 * seconds how can two points in the same track be 0.5 seconds apart?
 */
public class HighFrequencyPointRemover implements DataCleaner<Track> {

    final long minAllowableSpacingInMilliSec;

    public HighFrequencyPointRemover(Duration minAllowableSpacing) {
        this.minAllowableSpacingInMilliSec = minAllowableSpacing.toMillis();
    }

    /**
     * @param track An input track
     *
     * @return A Track that does not contain any two points that occur within the minimum allowable
     *     spacing specified at construction. Note: this rule can result in an empty Optional.
     */
    @Override
    public Optional<Track> clean(Track track) {

        NavigableSet<Point> points = newTreeSet(track.points());

        Set<Point> badPoints = findPointsWithoutEnoughTimeSpacing(track);

        points.removeAll(badPoints);

        return (points.isEmpty())
            ? Optional.empty()
            : Optional.of(new SimpleTrack(points));
    }

    /**
     * Identify all the Points in a Track that are too close together in time according to the
     * Duration parameter supplied during construction.
     *
     * @param track
     *
     * @return All the Points in this Track that are too close together in time.
     */
    public TreeSet<Point> findPointsWithoutEnoughTimeSpacing(Track track) {

        TreeSet<Point> badPoints = newTreeSet();

        Point lastPoint = null;
        Instant lastTime = Instant.MIN; //do not use null here, it triggers a "possible null reference" flag during a FAA code scan

        for (Point curPoint : track.points()) {

            //the first time through the loop set the "last" variables
            if (lastPoint == null) {
                lastTime = curPoint.time();
                lastPoint = curPoint;
                continue;
            }

            //in subsequent trips through the loop find points that are "too close"
            long timeDelta = curPoint.time().toEpochMilli() - lastTime.toEpochMilli();

            if (timeDelta < minAllowableSpacingInMilliSec) {
                badPoints.add(curPoint);
                badPoints.add(lastPoint);
            }

            lastTime = curPoint.time();
            lastPoint = curPoint;
        }

        return badPoints;
    }
}
