package org.mitre.openaria.core;

import org.mitre.caasd.commons.PositionRecord;


/**
 * A Positioner converts a raw record of type T into a PositionRecord<T>
 *
 * @param <T> A raw data type (that usually does not implement HasTime or HasPosition)
 */
@FunctionalInterface
public interface Positioner<T> {

    /**
     * Translate a raw record of type T into a PositionRecord<T> that can provide {time, latitude,
     * longitude, and altitude} data.
     */
    PositionRecord<T> asRecord(T raw);
}
