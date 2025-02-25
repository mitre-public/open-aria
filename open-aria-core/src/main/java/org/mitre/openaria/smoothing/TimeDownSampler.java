
package org.mitre.openaria.smoothing;

import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

/**
 * A TimeDownSampler will "thin out" a Track with a high-update rate (ie too many points per
 * minute). Reducing the number/frequency of points can be important for some analytics or processes
 * that are bogged down by high point density. (Down sampling to max rate of one point per 3.5
 * seconds is a good default value).
 * <p>
 * A DownSampler can also be used to remove Points that are out of steps with the "regular repeat
 * rate" of a surveillance system. For example, if "good data" has a Point every 5 seconds than
 * finding two points separated by just 2 seconds is likely to indicate that the "out of step" point
 * is flawed.
 */
public class TimeDownSampler<T> implements DataCleaner<Track<T>> {

    private final long minAllowableTimeDelta;

    /**
     * Create a DownSampler that will remove all "trailing" points that occur within a fixed
     * duration of an earlier point in the track. A DownSampler implicitly assumes all data is good,
     * this filter is not attempting to "pick the best" point. It "thins" a Track with a high-update
     * rate.
     *
     * @param minPointSeparation The minimum time spacing between to sequential points in a track.
     */
    public TimeDownSampler(Duration minPointSeparation) {
        this.minAllowableTimeDelta = minPointSeparation.toMillis();
    }

    @Override
    public Optional<Track<T>> clean(Track<T> track) {

        TreeSet<Point<T>> points = new TreeSet<>(track.points());

        Iterator<Point<T>> iter = points.iterator();

        Long tau = null;
        while (iter.hasNext()) {
            Point point = iter.next();

            //the 1st time through this loop set tau and ensure the 1st point isn't removed
            if (tau == null) {
                tau = point.time().toEpochMilli();
                continue;
            }

            long t = point.time().toEpochMilli();
            if ((t - tau) < minAllowableTimeDelta) {
                iter.remove();
            } else {
                tau = t;
            }
        }

        return Optional.of(Track.of(points));
    }
}
