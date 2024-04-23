package org.mitre.openaria.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparingInt;

import java.util.List;

import com.google.common.collect.HashMultiset;
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
}
