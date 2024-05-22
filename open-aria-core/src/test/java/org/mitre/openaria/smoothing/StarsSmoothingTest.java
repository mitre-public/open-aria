
package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopMessageType;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;

import org.junit.jupiter.api.Test;

public class StarsSmoothingTest {

    /*
     * starsrTrackWithCoastedPoints.txt is an example of Stars NOP data containing coasted and
     * dropped points.
     *
     * These static strings were taken from this example file.
     */
    public static final String ACTIVE_STARS = "[RH],STARS,MSO,10/07/2017,01:45:51.704,,,,0537,320,493,173,045.27869,-114.09840,3393,0000,0.1016,-98.4101,,,,MSO,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";
    public static final String COASTED_STARS = "[RH],STARS,MSO,10/07/2017,01:46:20.783,,,,0537,323,489,172,045.24810,-114.09186,3393,0000,0.3790,-100.2461,,,,MSO,,,,,,CST,IFR,,00000,,,,,,,1,,0,{RH}";
    public static final String DROPPED_STARS = "[RH],STARS,MSO,10/07/2017,01:46:41.983,,,,0537,323,489,172,045.21946,-114.08524,3393,0000,0.6602,-101.9648,,,,MSO,,,,,,DRP,IFR,,00000,,,,,,,1,,0,{RH}";

    @Test
    public void testIsCoastedPoint() {
        NopHit active = new NopHit(ACTIVE_STARS);
        NopHit coasted = new NopHit(COASTED_STARS);
        NopHit dropped = new NopHit(DROPPED_STARS);

        assertFalse(StarsSmoothing.isCoastedPoint(active));
        assertTrue(StarsSmoothing.isCoastedPoint(coasted));
        assertTrue(StarsSmoothing.isCoastedPoint(dropped));
    }

    @Test
    public void testIsCoastedRadarHit() {

        StarsRadarHit active = (StarsRadarHit) NopMessageType.parse(ACTIVE_STARS);
        StarsRadarHit coasted = (StarsRadarHit) NopMessageType.parse(COASTED_STARS);
        StarsRadarHit dropped = (StarsRadarHit) NopMessageType.parse(DROPPED_STARS);

        assertFalse(StarsSmoothing.isCoastedRadarHit(active));
        assertTrue(StarsSmoothing.isCoastedRadarHit(coasted));
        assertTrue(StarsSmoothing.isCoastedRadarHit(dropped));
    }
}
