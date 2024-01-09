
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

import org.mitre.openaria.core.Point;

public class HasNullSpeed implements Predicate<Point> {

    @Override
    public boolean test(Point pt) {
        return hasNullSpeed(pt);
    }

    public static boolean hasNullSpeed(Point point) {
        checkNotNull(point);
        return point.speedInKnots() == null || point.speedInKnots() <= 0.0;
    }
}
