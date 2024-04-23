package org.mitre.openaria.core.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EqualityVerifierTest {


    @Test
    public void properlyImprints() {
        EqualityVerifier<String> ev = new EqualityVerifier<>();
        ev.accept("a");

        assertThat(ev.imprintedValue(), is("a"));

        assertDoesNotThrow(() -> ev.accept("a"));
    }


    @Test
    public void rejectsItemsThatDontMatchImprint() {

        EqualityVerifier<String> ev = new EqualityVerifier<>();
        ev.accept("a");

        assertThrows(IllegalArgumentException.class, () -> ev.accept("b"));
    }

    @Test
    public void rejectsNullWhenConfigured() {

        EqualityVerifier<String> ev = new EqualityVerifier<>();
        assertThrows(IllegalArgumentException.class, () -> ev.accept(null));
    }

    @Test
    public void ignoresNullWhenConfigured() {

        EqualityVerifier<String> ev = new EqualityVerifier<>(true);
        assertDoesNotThrow(() -> ev.accept(null));

        ev.accept("b");
        assertThat(ev.imprintedValue(), is("b"));
    }

}