
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Objects.requireNonNull;

import java.util.NavigableSet;
import java.util.Optional;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;

/**
 * Filters out low speed points at the front and back of a track that have a common altitude
 * measurement. This filter can be useful when attempting to remove ground-data from Tracks (in
 * cases where you only are interested in the airborne section of a track).
 */
public class TrimSlowMovingPointsWithSimilarAltitudes implements DataCleaner<Track> {

    private final double speedLimitInKnots;
    private final Distance groundAltitudeTolerance;
    private final int minNumberPoints;

    /**
     * @param speedLimit         Points below this speed are removed from the front and back of the
     *                           track.
     * @param groundAltTolerance The amount a track's point can climb while still being considered
     *                           "on the ground" (assuming its speed is also low).
     * @param minNumberPoints    If the trimmed Track is smaller than this the entire Track is
     *                           removed
     */
    public TrimSlowMovingPointsWithSimilarAltitudes(Speed speedLimit, Distance groundAltTolerance, int minNumberPoints) {
        requireNonNull(speedLimit);
        requireNonNull(groundAltTolerance);
        checkArgument(speedLimit.isGreaterThan(Speed.ZERO), "The speed limit must be positive");
        checkArgument(minNumberPoints >= 1, "The minimum number of points must be at least 1");
        this.speedLimitInKnots = speedLimit.inKnots();
        this.groundAltitudeTolerance = groundAltTolerance;
        this.minNumberPoints = minNumberPoints;
    }

    @Override
    public Optional<Track> clean(Track track) {
        NavigableSet<Point> points = newTreeSet(track.points());

        removePointsFromBeginning(points);
        removePointsFromEnd(points);

        return (points.size() >= minNumberPoints)
            ? Optional.of(new SimpleTrack(points))
            : Optional.empty();
    }

    private void removePointsFromBeginning(NavigableSet<Point> points) {

        if (points.isEmpty()) {
            return;
        }
        //Use the first altitude measurement to estimate the ground level  -- SUPER WRONG IF AIRCRAFT IS ALREADY ALOFT
        Distance startingAlt = points.first().altitude();

        //remove low-speed points at the beginning of the track that share a common altitude
        while (!points.isEmpty() && isSlowAndInRange(points.first(), startingAlt)) {
            points.pollFirst();
        }
    }

    private void removePointsFromEnd(NavigableSet<Point> points) {

        if (points.isEmpty()) {
            return;
        }

        //Use the last altitude measurement to estimate the ground level -- SUPER WRONG IF AIRCRAFT DIDN'T LAND
        Distance endingAlt = points.last().altitude();

        while (!points.isEmpty() && isSlowAndInRange(points.last(), endingAlt)) {
            points.pollLast();
        }
    }

    private boolean isSlowAndInRange(Point p, Distance estimatedGroundAlt) {
        boolean isSlow = p.speedInKnots() < speedLimitInKnots;
        boolean hasSimilarAlt = p.altitude().minus(estimatedGroundAlt).abs().isLessThan(groundAltitudeTolerance);

        return isSlow && hasSimilarAlt;
    }
}
