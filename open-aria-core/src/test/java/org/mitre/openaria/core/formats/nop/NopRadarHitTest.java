package org.mitre.openaria.core.formats.nop;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NopRadarHitTest {


    @Test
    public void willRejectInputWithBadLongitude() {

        //longitude of -183 is illegal
        String badLongitude = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-183.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";

        assertThrows(IllegalArgumentException.class,
            () -> new NopRadarHitImpl(badLongitude)
        );
    }

    @Test
    public void willRejectInputWithBadLatitude() {

        //latitude of -91.27 is illegal
        String badLatitude = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,091.27719,-83.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";

        assertThrows(IllegalArgumentException.class,
            () -> new NopRadarHitImpl(badLatitude));
    }

    public static class NopRadarHitImpl extends NopRadarHit {

        public NopRadarHitImpl(String text) {
            super(text);
        }

        @Override
        public NopMessageType getNopType() {
            throw new UnsupportedOperationException();
        }
    }

}
