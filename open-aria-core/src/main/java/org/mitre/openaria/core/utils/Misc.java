package org.mitre.openaria.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mitre.caasd.commons.Time;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.openaria.core.Point;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;

/**
 * The convenience methods in this class should probably be moved to commons, given time
 */
public class Misc {

    /**
     * Build a "frequency sorted view" of a dataset
     *
     * @param iterable The data source
     * @param <T>      The type of elements
     *
     * @return A List of Multiset.Entry nodes that have been sorted by element count
     */
    public static <T> List<Multiset.Entry<T>> asMultiset(Iterable<T> iterable) {

        Multiset<T> multiset = HashMultiset.create(iterable);

        return multiset.entrySet().stream()
            .sorted(comparingInt(Multiset.Entry::getCount))
            .toList();
    }


    /** Find the most frequently occurring item and count. */
    public static <T> Multiset.Entry<T> mostCommonEntry(Iterable<T> iterable) {
        List<Multiset.Entry<T>> entries = asMultiset(iterable);

        checkArgument(!entries.isEmpty(), "Iterable cannot be empty");

        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }


    /** Find the least frequently occurring item and count. */
    public static <T> Multiset.Entry<T> leastCommonEntry(Iterable<T> iterable) {
        List<Multiset.Entry<T>> entries = asMultiset(iterable);

        checkArgument(!entries.isEmpty(), "Iterable cannot be empty");

        return entries.isEmpty() ? null : entries.get(0);
    }


    /** Find the most frequently occurring item. */
    public static <T> T mostCommon(Iterable<T> iterable) {
        return mostCommonEntry(iterable).getElement();
    }


    /** Find the least frequently occurring item. */
    public static <T> T leastCommon(Iterable<T> iterable) {
        return leastCommonEntry(iterable).getElement();
    }


    // @todo -- Remove when commons 58 is released
    /** @return The smallest possible TimeWindow that includes every Instant with a frequency count. */
    public static TimeWindow enclosingTimeWindow(Collection<Instant> times) {
        requireNonNull(times);
        checkArgument(!times.isEmpty());

        Instant minTime = Instant.MAX;
        Instant maxTime = Instant.MIN;

        for (Instant time : times) {
            minTime = Time.earliest(minTime, time);
            maxTime = Time.latest(maxTime, time);
        }

        return new TimeWindow(minTime, maxTime);
    }


    public static <T> Iterator<Point<?>> downCastPointIter(Iterator<Point<T>> typedIter) {
        // Use Guava to compose the original Iterator and a simple cast
        return Iterators.transform(
            typedIter,
            typedPoint -> (Point<?>) typedPoint
        );
    }
}
