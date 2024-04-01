package org.mitre.openaria.core.data;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSortedSet;

/**
 * A TrackRecord is a NavigableSet of Points. A TrackRecord can be made from a mutable collection
 * (e.g. TreeMap) or immutable collection (e.g. ImmutableSortedSet)
 *
 * @param points A NavigableSet of Points (e.g. ordered by the point's compare order)
 */
public record TrackRecord(NavigableSet<Point> points) implements Track {

    /**
     * Build a TrackRecord that copies this Collection of Points into an ImmutableSortedSet. This is
     * the preferred way to create Tracks.
     *
     * @param pointCol An arbitrary collection of points.
     */
    public static TrackRecord newTrackRecord(Collection<? extends Point> pointCol) {
        return new TrackRecord(ImmutableSortedSet.copyOf(pointCol));
    }

    /**
     * Build a TrackRec that copies this Collection of Points into a TreeSet.
     *
     * @param pointCol An arbitrary collection of points.
     */
    public static TrackRecord newMutableTrack(Collection<? extends Point> pointCol) {
        return new TrackRecord(new TreeSet<>(pointCol));
    }


    public TrackRecord asImmutable() {
        return new TrackRecord(ImmutableSortedSet.copyOf(points()));
    }

    public TrackRecord asMutable() {
        return new TrackRecord(new TreeSet<>(points()));
    }
}
