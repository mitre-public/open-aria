
package org.mitre.openaria.core;

import static com.google.common.collect.Sets.newTreeSet;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;

public class MutableTrack implements Track {

    private final NavigableSet<MutablePoint> points;

    public static MutableTrack of(Collection<MutablePoint> points) {
        return new MutableTrack(points);
    }

    public MutableTrack(Collection<MutablePoint> points) {
        this.points = newTreeSet(points);
    }

    @Override
    public NavigableSet<MutablePoint> points() {
        return points;
    }

    /**
     * @return A completely independent, and immutable, copy of this MutableTrack
     */
    public Track immutableCopy() {

        ArrayList<Point> immutablePoints = this.points().stream()
            .map(p -> Point.builder(p).build()) //copy to an immutable version
            .collect(toCollection(ArrayList::new));

        return new SimpleTrack(immutablePoints);
    }
}
