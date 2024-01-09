package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;

/**
 * This {@code Iterator<Point>} ensures the Points it provides only come from one facility
 */
public class SingleFacilityPointIterator implements Iterator<Point> {

    private final Iterator<Point> iter;

    String facility;

    /**
     * Create a SingleFacilityPointIterator when the required facility is known
     *
     * @param iterator A Point Iterator
     * @param requiredFacility This Point Iterator will fail if iter encounters a Point that does not have this facility
     */
    public SingleFacilityPointIterator(Iterator<Point> iterator, String requiredFacility) {
        this.iter = requireNonNull(iterator);
        this.facility = requiredFacility;
    }

    /**
     * Create a SingleFacilityPointIterator when the required facility is unknown.  The facility of
     * the first point returned by the iterator will be deemed the "correct" facility and from then
     * on out any Point that does not have this "correct" facility will fail
     *
     * @param iterator A Point Iterator=
     */
    public SingleFacilityPointIterator(Iterator<Point> iterator) {
        this(iterator, null);
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public Point next() {
        Point next = iter.next();
        if (facility == null) {
            facility = next.facility();
        } else {
            checkState(facility.equals(next.facility()));
        }
        return next;
    }
}

