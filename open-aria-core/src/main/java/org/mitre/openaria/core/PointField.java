
package org.mitre.openaria.core;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * This Enum (1) lists all Fields that may come inside a Point object, (2) assists the
 * implementation of PointBuilders by enforcing type constraints with the "accepts(Object)" method,
 * (3) assists "data extract" method using the "get(Point)" method.
 */
public enum PointField {

    TRACK_ID(String.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.trackId();
        }
    },
    LAT_LONG(LatLong.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.latLong();
        }
    },
    ALTITUDE(Distance.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.altitude();
        }
    },
    COURSE_IN_DEGREES(Double.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.course();
        }
    },
    SPEED(Double.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.speedInKnots();
        }
    },
    TIME(Instant.class) {
        @Override
        public Object get(Point aPoint) {
            return aPoint.time();
        }
    },;

    Class expectedType;

    private PointField(Class expectedType) {
        this.expectedType = expectedType;
    }

    public boolean accepts(Object value) {
        if (value == null) {
            return true;
        } else {
            return value.getClass().isAssignableFrom(expectedType);
        }
    }

    public abstract Object get(Point aPoint);

    /**
     * Parse a String that represents the value of a particular PointField.
     */
    public static Object parseString(PointField field, String token) {

        if (token.equals("") || token == null) {
            return null;
        }

        if (field.expectedType == String.class) {
            return token;
        } else if (field.expectedType == Double.class) {
            return Double.parseDouble(token);
        } else if (field.expectedType == Instant.class) {
            long time = Long.parseLong(token);
            return Instant.ofEpochMilli(time);
        }

        throw new AssertionError("Cannot parse the expectedType: " + field.expectedType.getName());
    }

    /**
     * Extract a field from a Point object, then convert that data point to a String
     */
    public static String toString(PointField field, Point aPoint) {

        Object value = field.get(aPoint);

        if (value == null) {
            return "";
        }

        if (field.expectedType == String.class) {
            return (String) value;
        } else if (field.expectedType == Double.class) {
            return ((Double) value).toString();
        } else if (field.expectedType == Instant.class) {
            long time = ((Instant) value).toEpochMilli();
            return Long.toString(time);
        }
        throw new AssertionError(
            "Cannot convert the expectedType: " + field.expectedType.getName() + " to a String"
        );
    }
}
