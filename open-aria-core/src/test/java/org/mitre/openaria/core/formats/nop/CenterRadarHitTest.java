package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;


public class CenterRadarHitTest {

    public static final String SAMPLE_1 = "[RH],Center,ZLA_B,07-10-2016,06:16:23.000,AAL350,B738,L,7305,176,381,319,34.0453,-119.1269,350,,,,,ZLA/15,,ZLA_B,,,,E0613,SFO,,IFR,,350,1396392188,LAX,0704,176//280,,L,1,,,{RH}";
    public static final String SAMPLE_2 = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";
    public static final String SAMPLE_3 = "[RH],Center,ZDC_B,07-10-2016,01:49:40.000,ICE644,B752,L,2151,298,463,046,39.6597,-75.2717,25F,,,,,ZDC/19,,ZDC_B,,,,D0131,BIKF,,IFR,,25F,1425487925,IAD,0647,298//350,,L,1,,,{RH}";

    @Test
    public void testCenterSample_1() {

        CenterRadarHit instance = new CenterRadarHit(SAMPLE_1);

        assertEquals(
            NopMessageType.CENTER_RADAR_HIT,
            instance.getNopType()
        );

        assertEquals(
            instance.rawMessage(),
            SAMPLE_1
        );

        assertEquals(
            "ZLA",
            instance.facility()
        );
        assertEquals(
            1468131383000L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );
        assertEquals(
            "AAL350",
            instance.callSign()
        );
        assertEquals(
            "B738",
            instance.aircraftType()
        );
        assertEquals(
            "L",
            instance.equipmentTypeSuffix()
        );
        assertEquals(
            "7305",
            instance.reportedBeaconCode()
        );
        assertThat(instance.altitudeInHundredsOfFeet(), is(176));
        assertThat(instance.speed(), is(381.0));
        assertThat(instance.heading(), is(319.0));
        assertThat(instance.latitude(), is(34.0453));
        assertThat(instance.longitude(), is(-119.1269));

        assertEquals(
            "350",
            instance.computerId()
        );
        assertEquals(
            "ZLA/15",
            instance.controllingFacilitySector()
        );
        assertEquals("ZLA_B",
            instance.sensorIdLetters()
        );
        assertEquals(
            "E0613",
            instance.coordinationTime()
        );
        assertEquals(
            "SFO",
            instance.arrivalAirport()
        );
        assertEquals(
            "IFR",
            instance.flightRules()
        );
        assertEquals(
            null,
            instance.cmsField153A()
        );
        assertEquals(
            "1396392188",
            instance.sequenceNumber()
        );
        assertEquals(
            "LAX",
            instance.departureAirport()
        );
        assertEquals(
            "0704",
            instance.eta()
        );
        assertEquals(
            "176//280",
            instance.reportInterimAssignAltitude()
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
    public void testCenterSampleWithNonIntegerComputerId() {

        CenterRadarHit rh = new CenterRadarHit(SAMPLE_3);
        NopTestUtils.triggerLazyParsing(rh);
        assertEquals("25F", rh.computerId());
    }

    @Test
    public void accessorMethodAlwaysReturnTheSameThing() {

        CenterRadarHit centerRh = (CenterRadarHit) NopMessageType.parse(SAMPLE_1);

        /*
         * Equality using == is intended.
         *
         * Accessing the same field from the same RHMessage twice should yield the EXACT SAME string
         *
         * This test exists because these String values are parsed lazily. The parsing process used
         * to produce a new instance of the output String each time, this wasted memory.
         */
        assertSame(centerRh.rawMessage(), centerRh.rawMessage());
        assertSame(centerRh.callSign(), centerRh.callSign());
        assertSame(centerRh.facility(), centerRh.facility());
        assertSame(centerRh.callSign(), centerRh.callSign());
        assertSame(centerRh.aircraftType(), centerRh.aircraftType());
        assertSame(centerRh.reportedBeaconCode(), centerRh.reportedBeaconCode());
        assertSame(centerRh.equipmentTypeSuffix(), centerRh.equipmentTypeSuffix());
        assertSame(centerRh.computerId(), centerRh.computerId());
        assertSame(centerRh.controllingFacilitySector(), centerRh.controllingFacilitySector());
        assertSame(centerRh.sensorIdLetters(), centerRh.sensorIdLetters());
        assertSame(centerRh.coordinationTime(), centerRh.coordinationTime());
        assertSame(centerRh.cmsField153A(), centerRh.cmsField153A());
        assertSame(centerRh.sequenceNumber(), centerRh.sequenceNumber());
        assertSame(centerRh.eta(), centerRh.eta());
        assertSame(centerRh.arrivalAirport(), centerRh.arrivalAirport());
        assertSame(centerRh.flightRules(), centerRh.flightRules());
        assertSame(centerRh.reportInterimAssignAltitude(), centerRh.reportInterimAssignAltitude());
        assertSame(centerRh.sequenceNumber(), centerRh.sequenceNumber());
        assertSame(centerRh.departureAirport(), centerRh.departureAirport());
        assertSame(centerRh.onActiveSensor(), centerRh.onActiveSensor());
        assertSame(centerRh.heavyLargeOrSmall(), centerRh.heavyLargeOrSmall());
        assertSame(centerRh.equipmentTypeSuffix(), centerRh.equipmentTypeSuffix());
    }

    @Test
    public void accessorMethodsShouldUseFlyweightPattern() {

        CenterRadarHit centerRh1 = (CenterRadarHit) NopMessageType.parse(SAMPLE_1);
        CenterRadarHit centerRh2 = (CenterRadarHit) NopMessageType.parse(SAMPLE_2);

        /*
         * Equality using == is intended.
         *
         * Ensure accessing an equal field from two RHs messages yeilds the EXACT same string when
         * appropriate.
         *
         * i.e. Don't waste memory putting the many different copies of common Strings like "IFR",
         * "VFR", airport codes, and facility names into memory.
         */
        assertSame(centerRh1.facility(), centerRh2.facility());
        assertSame(centerRh1.sensorIdLetters(), centerRh2.sensorIdLetters());
        assertSame(centerRh1.departureAirport(), centerRh2.departureAirport());
        assertSame(centerRh1.flightRules(), centerRh2.flightRules());
        assertSame(centerRh1.heavyLargeOrSmall(), centerRh2.heavyLargeOrSmall());
        assertSame(centerRh1.equipmentTypeSuffix(), centerRh2.equipmentTypeSuffix());
    }
}
