
package org.mitre.openaria.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;

import org.mitre.caasd.commons.Spherical;

import com.google.common.collect.Range;

/**
 * A ConflictAngle classifies just how two aircraft come into conflict with one another. Are they
 * traveling the same general direction? Are they crossing paths with one another? Or are they
 * traveling if roughly opposite directions?
 * <p>
 * This classification system comes directly from the FAA's internal definition.
 */
public enum ConflictAngle {

    SAME, //0 TO <45
    CROSSING, //45 TO 135
    OPPOSITE; //>135 TO 180

    public static ConflictAngle from(String string) {
        checkNotNull(string);
        return ConflictAngle.valueOf(string.trim().toUpperCase());
    }

    public static final Range VALID_COURSE_RANGE = Range.closed(0.0, 360.0);

    /**
     * @param c1 A course between 0 and 360
     * @param c2 A course between 0 and 360
     *
     * @return A classification of the angle between these courses
     */
    public static ConflictAngle beween(double c1, double c2) {
        checkArgument(VALID_COURSE_RANGE.contains(c1));
        checkArgument(VALID_COURSE_RANGE.contains(c2));

        return fromAngleDifference(
            Spherical.angleDifference(c1, c2)
        );
    }

    public static final Range VALID_ANGLE_DIFFERENCE_RANGE = Range.closed(-180.0, 180.0);

    /**
     * @param angleDifference An angle between -180 and 180
     *
     * @return A classification of this angle
     */
    public static ConflictAngle fromAngleDifference(double angleDifference) {
        checkArgument(VALID_ANGLE_DIFFERENCE_RANGE.contains(angleDifference));

        angleDifference = abs(angleDifference);

        if (angleDifference < 45) {
            return SAME;
        } else if (angleDifference <= 135) {
            return CROSSING;
        } else {
            return OPPOSITE;
        }
    }

}
