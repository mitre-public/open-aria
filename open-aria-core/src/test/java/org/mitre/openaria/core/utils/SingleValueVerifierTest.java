package org.mitre.openaria.core.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SingleValueVerifierTest {


    @Test
    public void properlyImprints() {
        SingleValueVerifier<String> svv = new SingleValueVerifier<>();
        svv.accept("a");

        assertThat(svv.imprintedValue(), is("a"));

        assertDoesNotThrow(() -> svv.accept("a"));
    }

    @Test
    public void rejectsItemsThatDontMatchImprint() {

        SingleValueVerifier<String> svv = new SingleValueVerifier<>();
        svv.accept("a");

        assertThat(svv.imprintedValue(), is("a"));
        assertThrows(IllegalArgumentException.class, () -> svv.accept("b"));
    }

    @Test
    public void rejectsNullWhenConfigured() {

        SingleValueVerifier<String> svv = new SingleValueVerifier<>();
        assertThrows(IllegalArgumentException.class, () -> svv.accept(null));
    }

    @Test
    public void ignoresNullWhenConfigured() {

        SingleValueVerifier<String> svv = new SingleValueVerifier<>(true);
        assertDoesNotThrow(() -> svv.accept(null));

        svv.accept("b");
        assertThat(svv.imprintedValue(), is("b"));
    }
}