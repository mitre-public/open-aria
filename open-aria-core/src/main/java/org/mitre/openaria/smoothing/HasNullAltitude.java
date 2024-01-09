
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

import org.mitre.openaria.core.Point;

public class HasNullAltitude implements Predicate<Point> {

    @Override
    public boolean test(Point pt) {
        return hasNullAltitude(pt);
    }

    private static boolean hasNullAltitude(Point point) {
        checkNotNull(point);
        return point.altitude() == null;
    }
}
