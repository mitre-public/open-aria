package org.mitre.openaria.core.metadata;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

public enum Direction {

    NORTH(0.0, "N"),
    NORTH_NORTH_EAST(22.5, "NNE"),
    NORTH_EAST(45.0, "NE"),
    EAST_NORTH_EAST(67.5, "ENE"),
    EAST(90, "E"),
    EAST_SOUTH_EAST(112.5, "ESE"),
    SOUTH_EAST(135.0, "SE"),
    SOUTH_SOUTH_EAST(157.5, "SSE"),
    SOUTH(180, "S"),
    SOUTH_SOUTH_WEST(202.5, "SSW"),
    SOUTH_WEST(225.0, "SW"),
    WEST_SOUTH_WEST(247.5, "WSW"),
    WEST(270, "W"),
    WEST_NORTH_WEST(292.5, "WNW"),
    NORTH_WEST(315.0, "NW"),
    NORTH_NORTH_WEST(337.5, "NNW");

    final double degrees;

    final String abbreviation;

    Direction(double degrees, String abbreviation) {
        this.degrees = degrees;
        this.abbreviation = abbreviation;
    }

    public double degrees() {
        return degrees;
    }

    public String abbrev() {
        return abbreviation;
    }

    public static Direction approxDirectionOf(double degrees) {
        checkArgument(degrees >= -360, "Input is too small (below -360 degrees)");
        checkArgument(degrees <= 360, "Input is too big (above 360 degrees)");
        if (degrees < 0) {
            degrees = 360 + degrees;
        }
        checkArgument(0 <= degrees && degrees <= 360);

        for (Direction dir : Direction.values()) {
            double difference = abs(degrees - dir.degrees);
            if (difference <= 11.25) {
                return dir;
            }
        }
        //you get here if the input was near 359 degrees.  In this case you won't select N or NNW.  You must select N manually
        return NORTH;
    }

}
