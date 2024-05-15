
package org.mitre.openaria.smoothing;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.Pair;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import com.google.common.base.Preconditions;

/**
 * A HasLowVariability Predicate identifies tracks that have a large number of Points that are
 * distributed across too few locations.
 * <p>
 * This Predicate is intended to find faux-tracks generated when radars misinterpret a radar returns
 * that reflected off a stationary objects (like a radio tower or tall building). This phenomenon
 * produces very long tracks that have very little movement.
 */
public class HasLowVariability<T> implements Predicate<Track<T>> {

    private final int trackSizeReq;
    private final double fracUniqueLocations;
    private final double latLongGridScalingFactor;

    /**
     * Create a Predicate that identifies tracks that have at least 1000 points that map to no more
     * than 20% (of the track size) unique locations. In other words, find long tracks that move
     * very little.
     */
    public HasLowVariability() {
        this(1000, 0.2, 1e5);
    }

    /**
     * Create a predicate that identifies tracks with low variability.
     *
     * @param trackSizeReq             The required number of points in the track to apply the test
     * @param fracUniqueLocations      The fraction of (unique locations) / (number of points) a
     *                                 track must occupy to not have low variability
     * @param latLongGridScalingFactor The scaling factor which determines how far apart two points
     *                                 need to be in order to have unique locations. With a small
     *                                 scaling factor, the test will determine more tracks have low
     *                                 variability.
     */
    public HasLowVariability(int trackSizeReq, double fracUniqueLocations, double latLongGridScalingFactor) {
        Preconditions.checkArgument(trackSizeReq > 0, "trackSizeReq must be positive");
        Preconditions.checkArgument(fracUniqueLocations >= 0, "The fraction of unique location must be postive");
        Preconditions.checkArgument(fracUniqueLocations <= 1, "The fraction of unique location cannot be more than 1");
        Preconditions.checkArgument(latLongGridScalingFactor > 0, "The scaling factor must be positive.");
        this.trackSizeReq = trackSizeReq;
        this.fracUniqueLocations = fracUniqueLocations;
        this.latLongGridScalingFactor = latLongGridScalingFactor;
    }

    @Override
    public boolean test(Track<T> track) {
        if (track.size() < trackSizeReq) {
            return false;
        }

        Set<Pair<Integer, Integer>> positions = track.points()
            .stream()
            .map(x -> getRoundedPositionValue(x))
            .collect(Collectors.toSet());

        //values as double, not ints
        double numUniquePositions = positions.size();
        double numPoints = track.size();
        double ratio = numUniquePositions / numPoints;

        return ratio <= fracUniqueLocations;
    }

    private Pair<Integer, Integer> getRoundedPositionValue(Point<T> pt) {
        return Pair.of(
            (int) (pt.latLong().latitude() * latLongGridScalingFactor),
            (int) (pt.latLong().longitude() * latLongGridScalingFactor)
        );
    }
}
