package org.mitre.openaria.smoothing;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.Pair;

import com.google.common.base.Preconditions;


/**
 * The {@code HasSmallTraversalRegion} identifies tracks that don't traverse enough grid squares,
 * i.e. they simply don't "cover enough ground", per the constructor parameters specification.
 * <p>
 * This Predicate is supposed to find fake tracks that are typically caused by radar reflections of
 * stationary objects. Since the data may be very noisy and have many points (e.g. especially in the
 * case of ASDE-X data), the {@link HasLowVariability} filter does not do a great job of identifying
 * very long AND very noisy tracks without rejecting too many real tracks). This filter is an
 * alternative to HasLowVariability.
 * <p>
 * The grid scaling is an easy way up to N decimal places (when using 1*10^N). For ballpark
 * distances, check out https://en.wikipedia.org/wiki/Decimal_degrees
 */
public class HasSmallTraversalRegion<T extends Track> implements Predicate<T> {

    private final int maxGridSquaresToTraverse;
    private final double latLongGridScalingFactor;

    public HasSmallTraversalRegion() {

        this(2, 1e3);
    }

    /**
     * Create a predicate that identifies tracks with low variability.
     *
     * @param maxGridSquaresToTraverse The maximum number of "square" regions with some track points
     *                                 inside them in order to still be considered a track with a
     *                                 "small traversal region".
     * @param latLongGridScalingFactor The scaling factor which determines how far apart two points
     *                                 need to be in order to have unique locations. With a small
     *                                 scaling factor, the test will determine more tracks have low
     *                                 variability.
     */
    public HasSmallTraversalRegion(int maxGridSquaresToTraverse,
        double latLongGridScalingFactor) {

        Preconditions.checkArgument(maxGridSquaresToTraverse >= 1, "The max grid squares to traverse must be >= 1");
        Preconditions.checkArgument(latLongGridScalingFactor > 0, "The scaling factor must be positive.");
        this.maxGridSquaresToTraverse = maxGridSquaresToTraverse;
        this.latLongGridScalingFactor = latLongGridScalingFactor;
    }

    @Override
    public boolean test(T track) {

        int numGridSquaresTraversed = track.points().stream()
            .map(this::roundedPositionValue)
            .collect(Collectors.toSet())
            .size();

        return numGridSquaresTraversed <= maxGridSquaresToTraverse;
    }

    private Pair<Integer, Integer> roundedPositionValue(Point pt) {
        return Pair.of(
            (int) (pt.latLong().latitude() * latLongGridScalingFactor),
            (int) (pt.latLong().longitude() * latLongGridScalingFactor)
        );
    }

}
