
package org.mitre.openaria.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;

import org.junit.jupiter.api.Test;


public class IfrVfrStatusTest {

    @Test
    public void testFrom() {
        assertThat(IfrVfrStatus.from("ifr"), is(IFR));
        assertThat(IfrVfrStatus.from("IFR"), is(IFR));
        assertThat(IfrVfrStatus.from("vfr"), is(VFR));
        assertThat(IfrVfrStatus.from("VFR"), is(VFR));
        assertThat(IfrVfrStatus.from("otp"), is(IFR));
        assertThat(IfrVfrStatus.from("OTP"), is(IFR));
    }
}
