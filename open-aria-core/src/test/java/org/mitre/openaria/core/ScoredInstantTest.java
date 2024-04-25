package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class ScoredInstantTest {

    @Test
    public void testEqualsAndHashCode() {
        EqualsVerifier.forClass(ScoredInstant.class).verify();
    }

    @Test
    public void testJsonWritable() {
        ScoredInstant test = ScoredInstant.of(12.12, Instant.ofEpochMilli(1617025242521L));
        assertEquals(test, ScoredInstant.fromJson(test.asJson()));
    }
}