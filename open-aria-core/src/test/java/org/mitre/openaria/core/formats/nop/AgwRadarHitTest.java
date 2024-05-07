package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


public class AgwRadarHitTest {

    public static final String SAMPLE_1 = "[RH],AGW,ABI_B,07/12/2016,19:21:08.848,N832AT,PA44,,5136,101,144,251,032.62683,-099.43983,088,5136,9.69,15.09,1,B,0,ABI,MAF,MWL,BGS,,MAF,,IFR,,39,39,TKI,,00,,S,0,,0,,94.59,96.59,{RH}";
    public static final String SAMPLE_2 = "[RH],AGW,ABI_B,07/12/2016,19:21:19.384,N2233W,PA28,,6276,066,96,266,032.31720,-098.82792,209,6276,42.77,0.59,1,B,2,ABI,L,MWL,ABIA,,ABI,,IFR,,188,69,JEN276015,,00,,S,0,V,0,,125.69,78.2,{RH}";

    @Test
    public void testStarsSample_1() {

        AgwRadarHit instance = new AgwRadarHit(SAMPLE_1);

        assertEquals(
            NopMessageType.AGW_RADAR_HIT,
            instance.getNopType()
        );

        assertEquals(
            instance.rawMessage(),
            SAMPLE_1
        );

        assertEquals(
            "ABI", //was ABI_B in original
            instance.facility()
        );
        assertEquals(
            1468351268000L + 848L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );

        assertEquals(
            "N832AT",
            instance.callSign()
        );
        assertEquals(
            "PA44",
            instance.aircraftType()
        );
        assertNull(instance.equipmentTypeSuffix());
        assertEquals(
            "5136",
            instance.reportedBeaconCode()
        );

        assertThat(instance.altitudeInHundredsOfFeet(), is(101));
        assertThat(instance.speed(), is(144.0));
        assertThat(instance.heading(), is(251.0));

        assertThat(instance.latitude(), is(032.62683));
        assertThat(instance.longitude(), is(-099.43983));
        assertEquals(
            "088",
            instance.trackNumber()
        );
        assertThat(instance.assignedBeaconCode(), is(5136));
        assertThat(instance.x(), is(9.69));
        assertThat(instance.y(), is(15.09));
        assertEquals(
            "1",
            instance.keyboard()
        );
        assertEquals(
            "B",
            instance.positionSymbol()
        );
        assertEquals(
            "0",
            instance.arrivalDepartureStatus()
        );
        assertEquals(
            "ABI",
            instance.sensorIdLetters()
        );
        assertEquals(
            "MAF",
            instance.scratchpad1()
        );
        assertEquals(
            "MWL",
            instance.entryFix()
        );
        assertEquals(
            "BGS",
            instance.exitFix()
        );
        assertEquals(
            "MAF",
            instance.arrivalAirport()
        );
        assertEquals(
            "IFR",
            instance.flightRules()
        );
        assertEquals(
            "39",
            instance.fdfNumber()
        );
        assertEquals(
            "39",
            instance.sequenceNumber()
        );
        assertEquals(
            "TKI",
            instance.departureAirport()
        );
        assertEquals(
            "00",
            instance.sensor()
        );
        assertNull(instance.scratchpad2());
        assertEquals(
            "S",
            instance.heavyLargeOrSmall()
        );
        assertEquals(
            false,
            instance.onActiveSensor()
        );
        assertNull(instance.additionalFacilityAlphaChar());
    }

    @Test
    public void accessorMethodAlwaysReturnTheSameThing() {

        AgwRadarHit agwRh1 = (AgwRadarHit) NopMessageType.parse(SAMPLE_1);

        /*
         * Equality using == is intended.
         *
         * Accessing the same field from the same RHMessage twice should yield the EXACT SAME string
         *
         * This test exists because these String values are parsed lazily. The parsing process used
         * to produce a new instance of the output String each time, this wasted memory.
         */
        assertSame(agwRh1.rawMessage(), agwRh1.rawMessage());
        assertSame(agwRh1.callSign(), agwRh1.callSign());
        assertSame(agwRh1.facility(), agwRh1.facility());
        assertSame(agwRh1.callSign(), agwRh1.callSign());
        assertSame(agwRh1.aircraftType(), agwRh1.aircraftType());
        assertSame(agwRh1.reportedBeaconCode(), agwRh1.reportedBeaconCode());
        assertSame(agwRh1.trackNumber(), agwRh1.trackNumber());
        assertSame(agwRh1.keyboard(), agwRh1.keyboard());
        assertSame(agwRh1.positionSymbol(), agwRh1.positionSymbol());
        assertSame(agwRh1.arrivalDepartureStatus(), agwRh1.arrivalDepartureStatus());
        assertSame(agwRh1.sensorIdLetters(), agwRh1.sensorIdLetters());
        assertSame(agwRh1.scratchpad1(), agwRh1.scratchpad1());
        assertSame(agwRh1.entryFix(), agwRh1.entryFix());
        assertSame(agwRh1.exitFix(), agwRh1.exitFix());
        assertSame(agwRh1.arrivalAirport(), agwRh1.arrivalAirport());
        assertSame(agwRh1.flightRules(), agwRh1.flightRules());
        assertSame(agwRh1.fdfNumber(), agwRh1.fdfNumber());
        assertSame(agwRh1.sequenceNumber(), agwRh1.sequenceNumber());
        assertSame(agwRh1.departureAirport(), agwRh1.departureAirport());
        assertSame(agwRh1.sensor(), agwRh1.sensor());
        assertSame(agwRh1.scratchpad2(), agwRh1.scratchpad2());
        assertSame(agwRh1.heavyLargeOrSmall(), agwRh1.heavyLargeOrSmall());
        assertSame(agwRh1.equipmentTypeSuffix(), agwRh1.equipmentTypeSuffix());
        assertSame(agwRh1.additionalFacilityAlphaChar(), agwRh1.additionalFacilityAlphaChar());
    }

    @Test
    public void accessorMethodsShouldUseFlyweightPattern() {

        AgwRadarHit agwRh1 = (AgwRadarHit) NopMessageType.parse(SAMPLE_1);
        AgwRadarHit agwRh2 = (AgwRadarHit) NopMessageType.parse(SAMPLE_2);

        /*
         * Equality using == is intended.
         *
         * Ensure accessing an equal field from two RHs messages yeilds the EXACT same string when
         * appropriate.
         *
         * i.e. Don't waste memory putting the many different copies of common Strings like "IFR",
         * "VFR", airport codes, and facility names into memory.
         */
        assertSame(agwRh1.facility(), agwRh2.facility());
        assertSame(agwRh1.keyboard(), agwRh2.keyboard());
        assertSame(agwRh1.positionSymbol(), agwRh2.positionSymbol());
        assertSame(agwRh1.sensorIdLetters(), agwRh2.sensorIdLetters());
        assertSame(agwRh1.entryFix(), agwRh2.entryFix());
        assertSame(agwRh1.flightRules(), agwRh2.flightRules());
        assertSame(agwRh1.sensor(), agwRh2.sensor());
        assertSame(agwRh1.scratchpad2(), agwRh2.scratchpad2());
        assertSame(agwRh1.heavyLargeOrSmall(), agwRh2.heavyLargeOrSmall());
        assertSame(agwRh1.equipmentTypeSuffix(), agwRh2.equipmentTypeSuffix());
    }
}
