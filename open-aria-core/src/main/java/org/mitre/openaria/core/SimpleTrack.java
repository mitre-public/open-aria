
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import com.google.common.collect.ImmutableSortedSet;

/**
 * A SimpleTrack maintains a fixed Collection of Points. These points may be instantiations of one
 * or more classes that implement the Point interface.
 */
public class SimpleTrack implements Track {

    private static final long serialVersionUID = 1L;

    private final ImmutableSortedSet<Point> points;

    public SimpleTrack(Collection<? extends Point> points) {
        checkNotNull(points);
        checkArgument(!points.isEmpty(), "The collection of input points cannot be empty");
        this.points = ImmutableSortedSet.copyOf(points);
    }

    /**
     * @return A "simple" {@code NavigableSet<Point>} that contains all the Points in the Track.
     *     Returning point data as regular Point objects encourages client code to rely solely on
     *     the Point interface and not on the specifics of any possible implementation of the Point
     *     interface.
     */
    @Override
    public ImmutableSortedSet<Point> points() {
        return points;
    }

    @Override
    public int size() {
        /*
         * This is faster than calling super.size() because the default implementation of size()
         * will lead to creating a defensive copy of the entire "points" dataset
         */
        return points.size();
    }
}
