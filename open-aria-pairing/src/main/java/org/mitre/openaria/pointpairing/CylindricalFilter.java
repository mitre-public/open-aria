
package org.mitre.openaria.pointpairing;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import java.util.function.Predicate;

import org.mitre.caasd.commons.Pair;
import org.mitre.openaria.core.Point;

/**
 * A CylindricalFilter tells us when two points are both (A) within a certain vertical distance and
 * (B) within a certain horizontal distance. Note, time is ignored in this comparison.
 */
public class CylindricalFilter implements Predicate<Pair<Point, Point>> {

    private final double maxHorizontalSeparationInFt;

    private final double maxVerticalSeparationInFt;

    /**
     * When this flag is set to true the filter will ignore the vertical dimension when altitude
     * data is missing.
     */
    private final boolean allowMissingAlt;

    public CylindricalFilter(double maxHorizontalSeparationInFt, double maxVerticalSeparationInFt, boolean allowMissingAlt) {
        checkArgument(maxHorizontalSeparationInFt >= 0, "Max Horizontal Separation must be non-negative");
        checkArgument(maxVerticalSeparationInFt >= 0, "Max Vertical Separation must be non-negative");

        this.maxHorizontalSeparationInFt = maxHorizontalSeparationInFt;
        this.maxVerticalSeparationInFt = maxVerticalSeparationInFt;
        this.allowMissingAlt = allowMissingAlt;
    }

    /**
     * Create a CylindricalFilter with pre-specific vertical and horizontal max separation. This
     * filter rejects all Point pairs that do not have altitude data.
     *
     * @param maxHorizontalSeparationInFt
     * @param maxVerticalSeparationInFt
     */
    public CylindricalFilter(double maxHorizontalSeparationInFt, double maxVerticalSeparationInFt) {
        this(maxHorizontalSeparationInFt, maxVerticalSeparationInFt, false);
    }

    @Override
    public boolean test(Pair<Point, Point> pair) {
        return testVertical(pair) && testHorizontal(pair);
    }

    private boolean testVertical(Pair<Point, Point> pair) {

        boolean altitudeDataIsMissing
            = pair.first().altitude() == null
            || pair.second().altitude() == null;

        if (altitudeDataIsMissing) {
            return allowMissingAlt;
        }

        double verticalSep = abs(pair.first().altitude().inFeet() - pair.second().altitude().inFeet());
        return verticalSep <= maxVerticalSeparationInFt;
    }

    private boolean testHorizontal(Pair<Point, Point> pair) {
        double horizontalSep = pair.first().distanceInNmTo(pair.second());
        return horizontalSep * feetPerNM() <= maxHorizontalSeparationInFt;

    }

}
