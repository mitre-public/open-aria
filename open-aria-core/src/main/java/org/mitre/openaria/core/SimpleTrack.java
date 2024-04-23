
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * A SimpleTrack maintains a fixed Collection of Points. These points may be instantiations of one
 * or more classes that implement the Point interface.
 */
public class SimpleTrack implements Track {

    private final NavigableSet<Point> points;

    public SimpleTrack(Collection<? extends Point> points) {
        checkNotNull(points);
        checkArgument(!points.isEmpty(), "The collection of input points cannot be empty");
        this.points = Collections.unmodifiableNavigableSet(new TreeSet<>(points));
    }

    /**
     * @return A "simple" {@code NavigableSet<Point>} that contains all the Points in the Track.
     *     Returning point data as regular Point objects encourages client code to rely solely on
     *     the Point interface and not on the specifics of any possible implementation of the Point
     *     interface.
     */
    @Override
    public NavigableSet<Point> points() {
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
