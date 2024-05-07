package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;



public class StarsRadarHitTest {

    public static final String SAMPLE_1 = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String SAMPLE_2 = "[RH],STARS,BIL,10/18/2016,00:06:18.097,,,,1200,000,000,xxx,047.00894,-109.34417,2640,0000,-32.9031,72.0203,,,,BIL,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";

    @Test
    public void testStarsSample_1() {

        StarsRadarHit instance = new StarsRadarHit(SAMPLE_1);

        assertEquals(
            NopMessageType.STARS_RADAR_HIT,
            instance.getNopType()
        );

        assertEquals(
            instance.rawMessage(),
            SAMPLE_1
        );

        assertEquals(
            "A80",
            instance.facility()
        );
        assertEquals(
            1468181033000L + 856L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );

        assertEquals(
            "DAL200",
            instance.callSign()
        );
        assertEquals(
            "MD88",
            instance.aircraftType()
        );
        assertEquals(
            "D",
            instance.equipmentTypeSuffix()
        );
        assertEquals(
            "1311",
            instance.reportedBeaconCode()
        );
        assertThat(instance.altitudeInHundredsOfFeet(), is(159));
        assertThat(instance.speed(), is(339.0));
        assertThat(instance.heading(), is(221.0));
        assertThat(instance.latitude(), is(034.27719));
        assertThat(instance.longitude(), is(-083.63591));

        assertEquals(
            "1519",
            instance.trackNumber()
        );
        assertThat(instance.assignedBeaconCode(), is(1311));
        assertThat(instance.x(), is(57.2078));
        assertThat(instance.y(), is(66.6181));
        assertEquals(
            "1",
            instance.keyboard()
        );
        assertEquals(
            "L",
            instance.positionSymbol()
        );
        assertEquals(
            "A",
            instance.arrivalDepartureStatus()
        );
        assertEquals(
            "A80",
            instance.sensorIdLetters()
        );
        assertNull(instance.scratchpad1());
        assertEquals(
            "DRE",
            instance.entryFix()
        );
        assertEquals(
            "ATL",
            instance.exitFix()
        );
        assertEquals(
            "2006",
            instance.ptdTime()
        );
        assertEquals(
            "ATL",
            instance.arrivalAirport()
        );
        assertEquals(
            "ACT",
            instance.trackStatus()
        );
        assertEquals(
            "IFR",
            instance.flightRules()
        );
        assertEquals(
            "01465",
            instance.systemFlightPlanNumber()
        );
        assertEquals(
            "L",
            instance.heavyLargeOrSmall()
        );
        assertEquals(
            true,
            instance.onActiveSensor()
        );
    }

    @Test
    public void testStarsSample_2() {

        StarsRadarHit instance = new StarsRadarHit(SAMPLE_2);

        assertEquals(
            "BIL",
            instance.facility()
        );
        assertEquals(
            1476749178000L + 97L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );

        assertNull(instance.callSign());
        assertNull(instance.aircraftType());
        assertNull(instance.equipmentTypeSuffix());
        assertEquals(
            "1200",
            instance.reportedBeaconCode()
        );
        assertThat(instance.altitudeInHundredsOfFeet(), is(0));
        assertThat(instance.speed(), is(0.0));

        assertThat("The orginal heading was xxx", instance.heading(), nullValue());

        assertThat(instance.latitude(), is(047.00894));
        assertThat(instance.longitude(), is(-109.34417));

        assertEquals(
            "2640",
            instance.trackNumber()
        );
        assertThat(instance.assignedBeaconCode(), is(0));
        assertThat(instance.x(), is(-32.9031));
        assertThat(instance.y(), is(72.0203));

        assertNull(instance.keyboard());
        assertNull(instance.positionSymbol());
        assertNull(instance.arrivalDepartureStatus());
        assertEquals(
            "BIL",
            instance.sensorIdLetters()
        );
        assertNull(instance.scratchpad1());
        assertNull(instance.entryFix());
        assertNull(instance.exitFix());
        assertNull(instance.ptdTime());
        assertNull(instance.arrivalAirport());
        assertEquals(
            "ACT",
            instance.trackStatus()
        );
        assertEquals(
            "IFR",
            instance.flightRules()
        );
        assertEquals(
            "00000",
            instance.systemFlightPlanNumber()
        );
        assertNull(instance.heavyLargeOrSmall());
        assertEquals(
            true,
            instance.onActiveSensor()
        );
    }


    @Test
    public void accessorMethodAlwaysReturnTheSameThing() {

        StarsRadarHit starsRh = (StarsRadarHit) NopMessageType.parse(SAMPLE_1);

        /*
         * Equality using == is intended.
         *
         * Accessing the same field from the same RHMessage twice should yield the EXACT SAME string
         *
         * This test exists because these String values are parsed lazily. The parsing process used
         * to produce a new instance of the output String each time, this wasted memory.
         */
        assertSame(starsRh.rawMessage(), starsRh.rawMessage());
        assertSame(starsRh.callSign(), starsRh.callSign());
        assertSame(starsRh.facility(), starsRh.facility());
        assertSame(starsRh.callSign(), starsRh.callSign());
        assertSame(starsRh.aircraftType(), starsRh.aircraftType());
        assertSame(starsRh.reportedBeaconCode(), starsRh.reportedBeaconCode());
        assertSame(starsRh.trackNumber(), starsRh.trackNumber());
        assertSame(starsRh.keyboard(), starsRh.keyboard());
        assertSame(starsRh.positionSymbol(), starsRh.positionSymbol());
        assertSame(starsRh.arrivalDepartureStatus(), starsRh.arrivalDepartureStatus());
        assertSame(starsRh.sensorIdLetters(), starsRh.sensorIdLetters());
        assertSame(starsRh.scratchpad1(), starsRh.scratchpad1());
        assertSame(starsRh.entryFix(), starsRh.entryFix());
        assertSame(starsRh.exitFix(), starsRh.exitFix());
        assertSame(starsRh.arrivalAirport(), starsRh.arrivalAirport());
        assertSame(starsRh.flightRules(), starsRh.flightRules());
        assertSame(starsRh.equipmentTypeSuffix(), starsRh.equipmentTypeSuffix());
        assertSame(starsRh.ptdTime(), starsRh.ptdTime());
        assertSame(starsRh.trackStatus(), starsRh.trackStatus());
        assertSame(starsRh.systemFlightPlanNumber(), starsRh.systemFlightPlanNumber());
        assertSame(starsRh.onActiveSensor(), starsRh.onActiveSensor());
        assertSame(starsRh.heavyLargeOrSmall(), starsRh.heavyLargeOrSmall());
        assertSame(starsRh.equipmentTypeSuffix(), starsRh.equipmentTypeSuffix());
    }

    @Test
    public void accessorMethodsShouldUseFlyweightPattern() {

        StarsRadarHit starsRh1 = (StarsRadarHit) NopMessageType.parse(SAMPLE_1);
        StarsRadarHit starsRh2 = (StarsRadarHit) NopMessageType.parse(SAMPLE_2);

        /*
         * Equality using == is intended.
         *
         * Ensure accessing an equal field from two RHs messages yeilds the EXACT same string when
         * appropriate.
         *
         * i.e. Don't waste memory putting the many different copies of common Strings like "IFR",
         * "VFR", airport codes, and facility names into memory.
         */
        assertSame(starsRh1.flightRules(), starsRh2.flightRules());
        assertSame(starsRh1.onActiveSensor(), starsRh2.onActiveSensor());
    }
}
