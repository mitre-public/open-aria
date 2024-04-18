package org.mitre.openaria.threading;

import org.mitre.openaria.core.KeyExtractor;
import org.mitre.openaria.core.Point;

/**
 * This class exists only to help bridge a refactor....do not use .... delete ASAP
 */
public class TempUtils {

    /** @return a KeyExtractor that generates String keys by concatenating trackId() and facility() */
    public static KeyExtractor<Point> keyExtractor() {
        return (Point p) -> p.trackId();
    }
}
