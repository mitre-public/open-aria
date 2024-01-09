
package org.mitre.openaria.core.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.core.utils.ConflictAngle.CROSSING;
import static org.mitre.openaria.core.utils.ConflictAngle.OPPOSITE;
import static org.mitre.openaria.core.utils.ConflictAngle.SAME;

import org.junit.jupiter.api.Test;



public class ConflictAngleTest {

    @Test
    public void betweenRejectBadInput_tooLow() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ConflictAngle.beween(-0.001, 10),
            "negative angles are rejected"
        );
    }

    @Test
    public void betweenRejectBadInput_tooHigh() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ConflictAngle.beween(360.001, 10),
            "angles over 360 are rejected"
        );
    }

    @Test
    public void sameDirection() {

        assertThat(ConflictAngle.beween(10.0, 10.0), is(SAME));
        assertThat(ConflictAngle.beween(360.0, 360.0), is(SAME));

        assertThat(ConflictAngle.beween(0, 44.9), is(SAME));
        assertThat(ConflictAngle.beween(44.9, 0), is(SAME));

        assertThat(ConflictAngle.beween(350, 10), is(SAME));
        assertThat(ConflictAngle.beween(10, 350), is(SAME));
    }

    @Test
    public void crossingAngles() {
        assertThat(ConflictAngle.beween(0, 45.0), is(CROSSING));
        assertThat(ConflictAngle.beween(45.0, 0), is(CROSSING));

        assertThat(ConflictAngle.beween(0, 135.0), is(CROSSING));
        assertThat(ConflictAngle.beween(135.0, 0), is(CROSSING));

        assertThat(ConflictAngle.beween(10, 145.0), is(CROSSING));
        assertThat(ConflictAngle.beween(145.0, 10), is(CROSSING));
    }

    @Test
    public void oppositeDirection() {

        assertThat(ConflictAngle.beween(0, 135.01), is(OPPOSITE));
        assertThat(ConflictAngle.beween(135.01, 0), is(OPPOSITE));

        assertThat(ConflictAngle.beween(0, 224.99), is(OPPOSITE));
        assertThat(ConflictAngle.beween(224.99, 0), is(OPPOSITE));

        assertThat(ConflictAngle.beween(10, 145.01), is(OPPOSITE));
        assertThat(ConflictAngle.beween(145.01, 10), is(OPPOSITE));
    }
}
