
package org.mitre.openaria.core;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * This class ONLY exists to make it easier to test classes and methods that should accept arbitrary
 * implementations of the Point interface.
 */
public class BasicPointImplementation_2<T> implements Point<T> {

    @Override
    public T rawData() {
        return null;
    }

    @Override
    public String trackId() {
        return null;
    }

    @Override
    public LatLong latLong() {
        return null;
    }

    @Override
    public Distance altitude() {
        return null;
    }

    @Override
    public Double course() {
        return null;
    }

    @Override
    public Double speedInKnots() {
        return null;
    }

    @Override
    public Instant time() {
        return Instant.EPOCH;
    }
}
