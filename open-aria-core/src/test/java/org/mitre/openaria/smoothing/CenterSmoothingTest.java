
package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.formats.nop.NopMessageType.parse;

import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Test;

public class CenterSmoothingTest {

    /*
     * centerTrackWithCoastedPoints.txt is an example of Center NOP data containing a coasted
     * points.
     *
     * These static strings were taken from this example file.
     */
    private static final String NON_COASTED_RH = "[RH],Center,ZNY,10-06-2017,18:59:21.000,GPD515,PC12,U,2434,008,088,279,41.0617,-73.6845,82N,,,,,NNN/00,,ZNY,,,,E1903,HPN,,VFR,,82N,1678646629,ACK,,008//105,,S,1,,,{RH}";
    private static final String COASTED_RH = "[RH],Center,ZNY,10-06-2017,19:00:23.000,GPD515,PC12,U,2434,006,086,280,41.0658,-73.7128,82N,,,,,NNN/00,,ZNY,,,,E1903,HPN,,VFR,C,82N,1678648777,ACK,,006//105,,S,1,,,{RH}";

    @Test
    public void testIsCoastedPoint() {
        NopHit notCoasted = new NopHit(NON_COASTED_RH);
        NopHit coasted = new NopHit(COASTED_RH);

        assertFalse(CenterSmoothing.isCoastedPoint(notCoasted));
        assertTrue(CenterSmoothing.isCoastedPoint(coasted));
    }

    @Test
    public void testIsCoastedRadarHit() {

        CenterRadarHit notCoasted = (CenterRadarHit) parse(NON_COASTED_RH);
        CenterRadarHit coasted = (CenterRadarHit) parse(COASTED_RH);

        assertFalse(CenterSmoothing.isCoastedRadarHit(notCoasted));
        assertTrue(CenterSmoothing.isCoastedRadarHit(coasted));
    }
}
