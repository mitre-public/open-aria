package org.mitre.openaria.core.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.core.utils.Misc.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class MiscTest {

    @Test
    public void mostCommonEntry_findsBest()  {

        List<String> list = List.of("a", "b", "a", "a");

        assertThat(mostCommonEntry(list).getElement(), is("a"));
        assertThat(mostCommonEntry(list).getCount(), is(3));

        assertThat(mostCommon(list), is("a"));
    }


    @Test
    public void leastCommonEntry_findsBest()  {

        List<String> list = List.of("a", "b", "a", "a");

        assertThat(leastCommonEntry(list).getElement(), is("b"));
        assertThat(leastCommonEntry(list).getCount(), is(1));

        assertThat(leastCommon(list), is("b"));
    }


    @Test
    public void mostCommonEntry_emptyInput()  {

        List<String> list = List.of();

        assertThrows(IllegalArgumentException.class, () -> mostCommonEntry(list));
        assertThrows(IllegalArgumentException.class, () -> mostCommon(list));
    }


    @Test
    public void leastCommonEntry_emptyInput()  {

        List<String> list = List.of();

        assertThrows(IllegalArgumentException.class, () -> leastCommonEntry(list));
        assertThrows(IllegalArgumentException.class, () -> leastCommon(list));
    }


}