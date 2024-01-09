
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newTreeSet;
import static org.mitre.caasd.commons.Time.durationBtw;
import static org.mitre.caasd.commons.Time.max;
import static org.mitre.caasd.commons.Time.theDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.TimeWindow;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class Points {

    /**
     * Dump the contents of a single Point to a Map.
     *
     * @param p A Point
     *
     * @return A Map that contains all the data within the point.
     */
    public static Map<PointField, Object> toMap(Point p) {

        HashMap<PointField, Object> map = new HashMap<>();

        for (PointField field : PointField.values()) {
            map.put(field, field.get(p));
        }
        return map;
    }

    /**
     * Find, and return the most common String value for a particular PointField across a collection
     * of Points
     *
     * @param field  A PointField that only accepts String
     * @param points The collection of points to count over
     *
     * @return The most value for a particular PointField
     */
    public static String mostCommon(PointField field, Collection<? extends Point> points) {

        if (field.expectedType != String.class) {
            throw new IllegalStateException("this method only works for String fields");
        }

        Multiset<String> multisetOfPointFields = multisetOf(field, points);
        return mostCommonEntry(multisetOfPointFields);
    }

    private static Multiset<String> multisetOf(PointField field, Collection<? extends Point> points) {
        Multiset<String> set = TreeMultiset.create();
        for (Point point : points) {
            String s = (String) field.get(point);
            if (s != null) {
                set.add(s);  //cannot add null to a multiset
            }
        }
        return set;
    }

    private static String mostCommonEntry(Multiset<String> set) {
        Multiset.Entry mostCommonEntry = null;
        for (Multiset.Entry<String> entry : set.entrySet()) {
            if (mostCommonEntry == null) {
                mostCommonEntry = entry;
            }

            if (mostCommonEntry.getCount() < entry.getCount()) {
                mostCommonEntry = entry;
            }
        }

        if (mostCommonEntry != null) {
            return (String) mostCommonEntry.getElement();
        } else {
            return null; //can be null if the set is empty (because the source points had no data)
        }
    }

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
    public static NavigableSet<Point> fastKNearestPoints(SortedSet<? extends Point> points, Instant time, int k) {

        checkNotNull(points, "The input SortedSet of Points cannot be null");
        checkNotNull(time, "The input time cannot be null");
        checkArgument(k >= 0, "k (" + k + ") must be non-negative");

        if (k >= points.size()) {
            return newTreeSet(points);
        }

        Point searchPoint = Point.builder().time(time).latLong(0.0, 0.0).build();

        //create two iterators, one goes up from the searchPoint, one goes down from the searchPoint
        NavigableSet<Point> headSet = ((NavigableSet<Point>) points).headSet(searchPoint, true);
        NavigableSet<Point> tailSet = ((NavigableSet<Point>) points).tailSet(searchPoint, false);
        Iterator<Point> headIter = headSet.descendingIterator();
        Iterator<Point> tailIter = tailSet.iterator();

        TreeSet<Point> results = newTreeSet();
        Point up = (headIter.hasNext()) ? headIter.next() : null;
        Point down = (tailIter.hasNext()) ? tailIter.next() : null;

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
    public static NavigableSet<Point> slowKNearestPoints(Collection<? extends Point> points, Instant time, int k) {
        checkNotNull(points, "The input collection of Points cannot be null");
        checkNotNull(time, "The input time cannot be null");
        checkArgument(k >= 0, "k (" + k + ") must be non-negative");

        if (k >= points.size()) {
            return newTreeSet(points);
        }
        if (k == 0) {
            return newTreeSet();
        }

        TreeSet<Point> bestSoFar = new TreeSet<>();
        Iterator<Point> iter = (Iterator<Point>) points.iterator();

        //seed with k pieces of data
        while (bestSoFar.size() < k) {
            bestSoFar.add(iter.next());
        }

        //the "next point" must be closer than this to go into the working solution
        Duration upperDelta = durationBtw(time, bestSoFar.first().time());
        Duration lowerDelta = durationBtw(time, bestSoFar.last().time());
        Duration addThreshold = max(upperDelta, lowerDelta);

        while (iter.hasNext()) {
            Point next = iter.next();

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
    static final Comparator<Comparable> NULLABLE_COMPARATOR = new Comparator<Comparable>() {

        @Override
        public int compare(Comparable o1, Comparable o2) {

            if (o1 != null && o2 != null) {
                //when both are not null return the comparision
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
    public static TreeSet<Point> subset(TimeWindow subsetWindow, NavigableSet<Point> points) {
        checkNotNull(subsetWindow);
        checkNotNull(points);

        //if the input collection is empty the output collection will be empty to
        if (points.isEmpty()) {
            return newTreeSet();
        }

        Point midPoint = Point.builder().time(subsetWindow.instantWithin(.5)).build();

        /*
         * Find exactly one point in the actual Track, ideally this point will be in the middle of
         * the time window
         */
        Point aPointInTrack = points.floor(midPoint);
        if (aPointInTrack == null) {
            aPointInTrack = points.ceiling(midPoint);
        }

        TreeSet<Point> outputSubset = newTreeSet();

        //given a starting point....go up until you hit startTime.
        NavigableSet<Point> headset = points.headSet(aPointInTrack, true);
        Iterator<Point> iter = headset.descendingIterator();
        while (iter.hasNext()) {
            Point pt = iter.next();
            if (subsetWindow.contains(pt.time())) {
                outputSubset.add(pt);
            }

            if (pt.time().isBefore(subsetWindow.start())) {
                break;
            }
        }

        //given a starting point....go down until you hit endTime.
        NavigableSet<Point> tailSet = points.tailSet(aPointInTrack, true);
        iter = tailSet.iterator();

        while (iter.hasNext()) {
            Point pt = iter.next();
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
