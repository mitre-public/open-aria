package org.mitre.openaria.core;

import org.apache.commons.math3.util.Precision;
import org.mitre.caasd.commons.Speed;

/**
 * Enum for classifying tracks based on climb rate
 */
public enum ClimbStatus {

    LEVEL,
    CLIMBING,
    DESCENDING;

    /**
     * Convert a climb rate for a given track to a climb status
     * @param climbRate Speed
     * @return the climb status
     */
    public static ClimbStatus fromClimbRate(Speed climbRate) {
        return ClimbStatus.fromClimbRate(climbRate, Speed.ofFeetPerSecond(.1));
    }

    /**
     * Convert climb rate to climb status with the provided tolerance for level flight
     */
    public static ClimbStatus fromClimbRate(Speed climbRate, Speed tolerance) {
        if (Precision.equals(climbRate.inFeetPerSecond(), 0.0, tolerance.inFeetPerSecond())) {
            return LEVEL;
        } else if (climbRate.isPositive()) {
            return CLIMBING;
        } else if (climbRate.isNegative()) {
            return DESCENDING;
        }
        throw new IllegalStateException("Could not convert a Speed to a ClimbStatus");
    }
}
