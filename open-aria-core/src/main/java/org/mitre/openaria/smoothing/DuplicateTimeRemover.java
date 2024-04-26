
package org.mitre.openaria.smoothing;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;

/**
 * If neighboring points have the same time value, the {@code DuplicateTimeRemover} will simply
 * remove the duplicate point from the {@link MutableTrack}.
 * <p>
 * Sometimes, ASDE data time stamps (which are rounded to nearest 1000 ms) are equal to each other.
 * When neighboring points have the same time value, certain calculations in various filters (e.g.
 * AlongTrackFilter) will attempt to infer values (e.g. heading, curvature) from a point with
 * itself, which returns NaN and blows up other things.
 */
public class DuplicateTimeRemover implements DataCleaner<MutableTrack> {

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        NavigableSet<Point> points = track.points();

        if (trackHasEnoughPoints(points)) {
            resolvePointTimes(points);
        }

        return points.isEmpty() ? Optional.empty() : Optional.of(track);
    }

    private boolean trackHasEnoughPoints(NavigableSet<Point> points) {
        return points.size() > 2;
    }

    private void resolvePointTimes(NavigableSet<Point> points) {

        Iterator<Point> iter = points.iterator();
        Point first = iter.next();
        Point second;

        while (iter.hasNext()) {
            second = iter.next();
            if (!second.time().isAfter(first.time())) {
                iter.remove();
            } else {
                first = second;
            }
        }
    }

}
