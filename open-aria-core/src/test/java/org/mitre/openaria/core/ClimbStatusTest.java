package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Speed;

public class ClimbStatusTest {

    @Test
    void testClimbStatus_forNegativeClimbRate_isDescending() {
        assertEquals(ClimbStatus.DESCENDING, ClimbStatus.fromClimbRate(Speed.ofKnots(-2.0)));
    }

    @Test
    void testClimbStatus_forPositiveClimbRate_isClimbing() {
        assertEquals(ClimbStatus.CLIMBING, ClimbStatus.fromClimbRate(Speed.ofKnots(2.0)));
    }

    @Test
    void testClimbStatus_forZeroClimbRate_isLevel() {
        assertEquals(ClimbStatus.LEVEL, ClimbStatus.fromClimbRate(Speed.ZERO));
    }

    @Test
    void testClimbStatus_forSmallPositiveClimbRate_isLevel() { assertEquals(ClimbStatus.LEVEL, ClimbStatus.fromClimbRate(Speed.ofFeetPerSecond(.001))); }

    @Test
    void testClimbStatus_forSmallNegativeClimbRate_isLevel() { assertEquals(ClimbStatus.LEVEL, ClimbStatus.fromClimbRate(Speed.ofFeetPerSecond(-.001))); }

    @Test
    void testClimbStatus_forLargeTolerance_isLevel() { assertEquals(ClimbStatus.LEVEL, ClimbStatus.fromClimbRate(Speed.ofFeetPerSecond(90), Speed.ofFeetPerSecond(100))); }
}
