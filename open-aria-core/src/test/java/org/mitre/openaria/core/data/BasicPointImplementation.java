package org.mitre.openaria.core.data;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;


/**
 * This class ONLY exists to make it easier to test classes and methods that should accept arbitrary
 * implementations of the Point interface.
 */
public class BasicPointImplementation implements Point {

    @Override
    public Object rawData() {
        return null;
    }

    @Override
    public String linkId() {
        return null;
    }

    @Override
    public Instant time() {
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
    public int compareTo(Object o) {
        return 0;
    }
}
