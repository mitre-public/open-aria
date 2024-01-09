package org.mitre.openaria.core.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.core.metadata.Direction.NORTH;
import static org.mitre.openaria.core.metadata.Direction.approxDirectionOf;

import org.junit.jupiter.api.Test;


public class DirectionTest {

    @Test
    public void approxDirectionWorksCorrectly() {
        //note: there are 22.5 degrees between each value
        //so there are 11.25 degrees to "either side" of a value

        for (Direction dir : Direction.values()) {
            assertThat(approxDirectionOf(dir.degrees - 11.249), is(dir));
            assertThat(approxDirectionOf(dir.degrees), is(dir));
            assertThat(approxDirectionOf(dir.degrees + 11.249), is(dir));
        }
    }

    @Test
    public void rejectValuesOver360() {
        assertThrows(
            IllegalArgumentException.class,
            () -> approxDirectionOf(360.1))
        ;
    }

    @Test
    public void rejectValuesBelowNegative360() {
        assertThrows(
            IllegalArgumentException.class,
            () -> approxDirectionOf(-360.1)
        );
    }

    @Test
    public void acceptValuePostiveAndNegative360() {
        assertThat(approxDirectionOf(360), is(NORTH));
        assertThat(approxDirectionOf(-360), is(NORTH));
    }
}
