
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.NavigableSet;
import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

/**
 * Filters out low speed points at the front and back of a track. This filter can be useful when
 * attempting to remove ground-data from Tracks (in cases where you only are interested in the
 * airborne section of a track).
 */
public class TrimLowSpeedPoints<T> implements DataCleaner<Track<T>> {

    private final double speedLimitInKnots;
    private final int minNumberPoints;

    /**
     * @param speedLimitInKnots Points below this speed are removed from the front and back of the
     *                          track.
     * @param minNumberPoints   If the trimmed Track is smaller than this the entire Track is
     *                          removed
     */
    public TrimLowSpeedPoints(double speedLimitInKnots, int minNumberPoints) {
        checkArgument(speedLimitInKnots > 0, "The speed limit must be positive");
        checkArgument(minNumberPoints >= 1, "The minimum number of points must be at least 1");
        this.speedLimitInKnots = speedLimitInKnots;
        this.minNumberPoints = minNumberPoints;
    }

    @Override
    public Optional<Track<T>> clean(Track<T> track) {
        NavigableSet<Point<T>> points = newTreeSet(track.points());
        //remove low-speed points at the start of the track
        while (!points.isEmpty() && points.first().speed().inKnots() < speedLimitInKnots) {
            points.pollFirst();
        }
        //remove low-speed points at the end of the trank
        while (!points.isEmpty() && points.last().speed().inKnots() < speedLimitInKnots) {
            points.pollLast();
        }
        return (points.size() >= minNumberPoints) ? Optional.of(Track.of(points)) : Optional.empty();
    }

}
