package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;
import static org.mitre.openaria.core.PairedIfrVfrStatus.IFR_IFR;
import static org.mitre.openaria.core.PairedIfrVfrStatus.IFR_VFR;
import static org.mitre.openaria.core.PairedIfrVfrStatus.VFR_VFR;

import org.junit.jupiter.api.Test;



public class PairedIfrVfrStatusTest {

    @Test
    public void fromRejectsEmptyList() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PairedIfrVfrStatus.from(newArrayList())
        );
    }

    @Test
    public void fromRejectsListOfSize1() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PairedIfrVfrStatus.from(newArrayList(IFR))
        );

    }

    @Test
    public void fromRejectsListOfSize3() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PairedIfrVfrStatus.from(newArrayList(IFR, IFR, VFR))
        );
    }

    @Test
    public void fromAcceptsListOfSize2() {
        PairedIfrVfrStatus.from(newArrayList(IFR, VFR));
    }

    @Test
    public void fromProvidesCorrectAnswers() {
        assertThat(PairedIfrVfrStatus.from(IFR, IFR), is(IFR_IFR));
        assertThat(PairedIfrVfrStatus.from(VFR, IFR), is(IFR_VFR));
        assertThat(PairedIfrVfrStatus.from(IFR, VFR), is(IFR_VFR));
        assertThat(PairedIfrVfrStatus.from(VFR, VFR), is(VFR_VFR));
    }
}
