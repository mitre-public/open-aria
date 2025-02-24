
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newTreeSet;
import static org.mitre.caasd.commons.Time.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.TimeWindow;

public class Points {

    /**
     * Find the k Points in this Collection with time values that are closest to the input time.
     * This method is significantly faster than its sister methods because it takes advantage of the
     * Sorted-ness of the input data. This method operates in O(k) time.
     *
     * @param points A SortedSet of Points that can be efficiently searched.
     * @param time   The time "anchor" for the kNN computation
     * @param k      The maximum number of Points that should be retrieved.
     *
     * @return A NavigableSet contain at most k Points from this Track.
     */
    public static <T> NavigableSet<Point<T>> fastKNearestPoints(SortedSet<Point<T>> points, Instant time, int k) {

        checkNotNull(points, "The input SortedSet of Points cannot be null");
        checkNotNull(time, "The input time cannot be null");
        checkArgument(k >= 0, "k (" + k + ") must be non-negative");

        if (k >= points.size()) {
            return newTreeSet(points);
        }

        Point<T> stub = points.first();
        Point<T> searchPoint = Point.builder(stub).time(time).latLong(0.0, 0.0).build();

        //create two iterators, one goes up from the searchPoint, one goes down from the searchPoint
        NavigableSet<Point<T>> headSet = ((NavigableSet<Point<T>>) points).headSet(searchPoint, true);
        NavigableSet<Point<T>> tailSet = ((NavigableSet<Point<T>>) points).tailSet(searchPoint, false);
        Iterator<Point<T>> headIter = headSet.descendingIterator();
        Iterator<Point<T>> tailIter = tailSet.iterator();

        TreeSet<Point<T>> results = newTreeSet();
        Point<T> up = (headIter.hasNext()) ? headIter.next() : null;
        Point<T> down = (tailIter.hasNext()) ? tailIter.next() : null;

        while (results.size() < k) {
            //add an element from the "down set" when we are out of elements in the "up set"
            if (up == null) {
                results.add(down);
                down = tailIter.next();
                continue;
            }
            //add an element from the "up set" when we are out of elements in the "down set"
            if (down == null) {
                results.add(up);
                up = headIter.next();
                continue;
            }
            //add the nearest point when we can choose between the "up set" and the "down set"
            Duration upDistance = Duration.between(up.time(), time);
            Duration downDistance = Duration.between(time, down.time());

            if (theDuration(upDistance).isLessThanOrEqualTo(downDistance)) {
                results.add(up);
                up = (headIter.hasNext()) ? headIter.next() : null;
            } else {
                results.add(down);
                down = (tailIter.hasNext()) ? tailIter.next() : null;
            }
        }

        return results;
    }

    /**
     * Find the k Points in this Collection with time values that are closest to the input time.
     * This method is significantly slower than its sister methods because it must search the entire
     * collection of input data to obtain an answer. This method operates in O(n) time.
     *
     * @param points A collection of Points to search through
     * @param time   The time "anchor" for the kNN computation
     * @param k      The maximum number of Points that should be retrieved.
     *
     * @return A NavigableSet contain at most k Points from this Track.
     */
    public static <T> NavigableSet<Point<T>> slowKNearestPoints(Collection<? extends Point<T>> points, Instant time, int k) {
        checkNotNull(points, "The input collection of Points cannot be null");
        checkNotNull(time, "The input time cannot be null");
        checkArgument(k >= 0, "k (" + k + ") must be non-negative");

        if (k >= points.size()) {
            return newTreeSet(points);
        }
        if (k == 0) {
            return newTreeSet();
        }

        TreeSet<Point<T>> bestSoFar = new TreeSet<>();
        Iterator<? extends Point<T>> iter = points.iterator();

        //seed with k pieces of data
        while (bestSoFar.size() < k) {
            bestSoFar.add(iter.next());
        }

        //the "next point" must be closer than this to go into the working solution
        Duration upperDelta = durationBtw(time, bestSoFar.first().time());
        Duration lowerDelta = durationBtw(time, bestSoFar.last().time());
        Duration addThreshold = max(upperDelta, lowerDelta);

        while (iter.hasNext()) {
            Point<T> next = iter.next();

            Duration delta = durationBtw(time, next.time());

            if (theDuration(delta).isLessThan(addThreshold)) {
                /* This element improves the working result. So Add it */
                bestSoFar.add(next);
                /* Recompute the upper and lower thresholds so we know which Point gets removed. */
                upperDelta = durationBtw(time, bestSoFar.first().time());
                lowerDelta = durationBtw(time, bestSoFar.last().time());

                //remove the k+1 element
                if (theDuration(upperDelta).isGreaterThanOrEqualTo(lowerDelta)) {
                    bestSoFar.pollFirst();
                    upperDelta = durationBtw(time, bestSoFar.first().time());
                } else {
                    bestSoFar.pollLast();
                    lowerDelta = durationBtw(time, bestSoFar.last().time());
                }
                addThreshold = max(upperDelta, lowerDelta);
            }
        }

        return newTreeSet(bestSoFar);
    }

    /**
     * This Comparator should only be used to help the implementation of Point.compareTo handle
     * Points with missing/null values. This Comparator does not properly handle cases where the
     * inputs are of different classes that should not be compared against one another.
     */
    static final Comparator<Comparable> NULLABLE_COMPARATOR = (o1, o2) -> {

        if (o1 != null && o2 != null) {
            //when both are not null return the comparison
            return o1.compareTo(o2);
        } else if (o1 == null && o2 == null) {
            //when both are null return 0
            return 0;
        } else if (o1 == null) {
            //when left is null return "right is greater"
            return -1;
        } else {
            //when right is null return "left is greater"
            return 1;
        }
    };

    /**
     * @param p1 A LatLong position and time
     * @param p2 A second LatLong position and time
     *
     * @return The Speed you would have to travel to go from the first point to the second point
     *     given the time that elapsed between the two Instants (Note this method ONLY reflections
     *     LatLong and Time data. Speed and Heading data is ignored)
     */
    public static Speed speedBetween(Point p1, Point p2) {
        return Speed.between(p1.latLong(), p1.time(), p2.latLong(), p2.time());
    }

    /**
     * Extract a time filtered subset of points from a potentially large set of Points. This method
     * iterates over as few points as possible because it relies on the sorted nature of incoming
     * points parameter to perform a quick extraction.
     *
     * @param subsetWindow Extract points that are within this window
     * @param points       A potentially large TreeSet of points
     *
     * @return A new TreeSet containing all the points that fall within the TimeWindow
     */
    public static <T> TreeSet<Point<T>> subset(TimeWindow subsetWindow, NavigableSet<Point<T>> points) {
        checkNotNull(subsetWindow);
        checkNotNull(points);

        //if the input collection is empty the output collection will be empty to
        if (points.isEmpty()) {
            return newTreeSet();
        }

        Point<T> midPoint = Point.<T>builder()
            .time(subsetWindow.instantWithin(.5))
            .latLong(0.0, 0.0)
            .build();

        /*
         * Find exactly one point in the actual Track, ideally this point will be in the middle of
         * the time window
         */
        Point<T> aPointInTrack = points.floor(midPoint);
        if (aPointInTrack == null) {
            aPointInTrack = points.ceiling(midPoint);
        }

        TreeSet<Point<T>> outputSubset = newTreeSet();

        //given a starting point....go up until you hit startTime.
        NavigableSet<Point<T>> headset = points.headSet(aPointInTrack, true);
        Iterator<Point<T>> iter = headset.descendingIterator();
        while (iter.hasNext()) {
            Point<T> pt = iter.next();
            if (subsetWindow.contains(pt.time())) {
                outputSubset.add(pt);
            }

            if (pt.time().isBefore(subsetWindow.start())) {
                break;
            }
        }

        //given a starting point....go down until you hit endTime.
        NavigableSet<Point<T>> tailSet = points.tailSet(aPointInTrack, true);
        iter = tailSet.iterator();

        while (iter.hasNext()) {
            Point<T> pt = iter.next();
            if (subsetWindow.contains(pt.time())) {
                outputSubset.add(pt);
            }

            if (pt.time().isAfter(subsetWindow.end())) {
                break;
            }
        }

        return outputSubset;
    }
}
