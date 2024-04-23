
package org.mitre.openaria.core;

import static com.google.common.collect.Sets.newTreeSet;
import static java.util.stream.Collectors.toCollection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;

import org.mitre.caasd.commons.TimeWindow;

public class MutableTrack {

    private final NavigableSet<MutablePoint> points;

    public static MutableTrack of(Collection<MutablePoint> points) {
        return new MutableTrack(points);
    }

    public MutableTrack(Collection<MutablePoint> points) {
        this.points = newTreeSet(points);
    }

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

        return Track.of( (ArrayList) immutablePoints);
    }


    public int size() {
        return points().size();
    }

    /**
     * @return The minimum TimeWindow that contains all Points within this Track."
     */
    public TimeWindow asTimeWindow() {
        NavigableSet<? extends Point> pts = points();
        return TimeWindow.of(
            pts.first().time(),
            pts.last().time()
        );
    }

    /**
     * Find the k Points in this Track with time values closest to the given input time.
     *
     * @param time The time "anchor" for the kNN computation
     * @param k    The maximum number of Points that should be retrieved.
     *
     * @return A NavigableSet contain at most k Points from this Track.
     */
    public NavigableSet<Point> kNearestPoints(Instant time, int k) {
        return Points.fastKNearestPoints( points(), time, k);
    }


    /**
     * Find the Point in this Track whose time value is closest to the given input time. This method
     * is equivalent to <code> kNearestPoints(time, 1).first(); </code>
     *
     * @param time The time "anchor" for the kNN computation
     *
     * @return Exactly 1 Point from this Track
     */
    public Point nearestPoint(Instant time) {
        return kNearestPoints(time, 1).first();
    }
}
