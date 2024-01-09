
package org.mitre.openaria.threading;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;

import org.mitre.openaria.core.Point;

/**
 * A TrackUnderConstruction is a collection of Points that have been assigned to the same track.
 * <p>
 * A TrackUnderConstruction intentionally does not help make the decisions about "whether a Point
 * should be added to this Track". This decision should be made elsewhere. The purpose of
 * TrackUnderConstruction is to simplify the implementation of other classes by allowing them to
 * create data-structures like List<TrackUnderConstruction> as opposed to
 * List<NavigableSet<Point>>.
 * <p>
 * Convenient access to the last Point in this track is provided to assist decisions about when to
 * "close" a TrackUnderConstruction.
 * <p>
 * This class is not public as it should not be used outside this package.
 */
class TrackUnderConstruction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LinkedList<Point> points;

    TrackUnderConstruction(Point firstPoint) {
        this.points = new LinkedList<>();
        this.points.add(firstPoint);
    }

    final void addPoint(Point point) {
        //we don't need to verify that this point occurs at or after the exist last point.
        //this is gauranteed by the TrackMaker ensuring that ALL points are in time order
        points.add(point);
    }

    Instant timeOfEarliestPoint() {
        return points.getFirst().time();
    }

    Instant timeOfLatestPoint() {
        return points.getLast().time();
    }

    Collection<Point> points() {
        return points;
    }

    public Point lastPoint() {
        return points.getLast();
    }

    public int size() {
        return points.size();
    }

    /**
     * @param timeAfterLastPointsTime An Instant that occurs after this Track's last point.
     *
     * @return The duration between the given time and the time associated with the last point (it
     *     time) added to this Track
     */
    Duration timeSince(Instant timeAfterLastPointsTime) {
        return Duration.between(this.timeOfLatestPoint(), timeAfterLastPointsTime);
    }
}
