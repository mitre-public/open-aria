
package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.smoothing.HasNullSpeed.hasNullSpeed;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;


public class HasNullSpeedTest {

    @Test
    public void testHasNullSpeed_nullSpeed() {
        Point pointWithNullSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .build();

        assertTrue(hasNullSpeed(pointWithNullSpeed));
    }

    @Test
    public void testHasNullSpeed_nullInput() {
        assertThrows(
            NullPointerException.class,
            () -> hasNullSpeed(null)
        );
    }

    @Test
    public void testHasNullSpeed_ZeroSpeed() {
        Point pointWithZeroSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .speed(0.0)
            .build();

        assertTrue(hasNullSpeed(pointWithZeroSpeed));
    }

    @Test
    public void testHasNullSpeed_goodSpeed() {

        Point pointWithZeroSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .speed(100.0)
            .build();

        assertFalse(hasNullSpeed(pointWithZeroSpeed));
    }

    @Test
    public void testPredicate() {

        Point pointWithNullSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .build();

        Point pointWithZeroSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .speed(0.0)
            .build();

        Point pointWithGoodSpeed = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .speed(100.0)
            .build();

        HasNullSpeed predicate = new HasNullSpeed();

        assertTrue(predicate.test(pointWithNullSpeed));
        assertTrue(predicate.test(pointWithZeroSpeed));
        assertFalse(predicate.test(pointWithGoodSpeed));
    }
}
