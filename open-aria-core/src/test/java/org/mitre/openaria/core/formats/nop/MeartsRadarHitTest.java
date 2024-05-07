package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class MeartsRadarHitTest {


    public static final String SAMPLE_1 = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";
    public static final String SAMPLE_2 = "[RH],MEARTS,ZUA_B,11-05-2019,15:30:02.020,,,,2000,390,455,142,016.1706,149.0943,,,499.43,417.5876,,/,,ZUA_B,,,,,,,,,,,,,390//,,,1,{RH}";

    @Test
    public void testMeartsSample_1() {

       MeartsRadarHit instance = new MeartsRadarHit(SAMPLE_1);

        assertEquals(
           NopMessageType.MEARTS_RADAR_HIT,
            instance.getNopType()
        );

        assertEquals(
            instance.rawMessage(),
            SAMPLE_1
        );

        assertEquals(
            "ZUA",
            instance.facility()
        );
        assertEquals(
            1572967686000L + 20L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );

        assertEquals(
            "UAL185",
            instance.callSign()
        );
        assertEquals(
            "B737",
            instance.aircraftType()
        );
        assertEquals(
            "L",
            instance.equipmentTypeSuffix()
        );
        assertEquals(
            "2646",
            instance.reportedBeaconCode()
        );
        assertThat(instance.altitudeInHundredsOfFeet(), is(400));
        assertThat(instance.speed(), is(450.0));
        assertThat(instance.heading(), is(239.0));
        assertThat(instance.latitude(), is(011.6384));
        assertThat(instance.longitude(), is(141.6778));
        assertThat(instance.x(), is(67.50287));
        assertThat(instance.y(), is(145.9169));

        assertEquals(
            "ZUA/1F",
            instance.controllingFacilitySector()
        );
        assertEquals(
            "ZUA_B",
            instance.sensorIdLetters()
        );
        assertEquals(
            null,
            instance.arrivalAirport()
        );
        assertEquals(
            null,
            instance.flightRules()
        );
        assertEquals(
            null,
            instance.departureAirport()
        );
        assertEquals(
            "E1430",
            instance.eta()
        );
        assertEquals(
            "400//400",
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
    public void testMeartsSample_2() {

       MeartsRadarHit instance = new MeartsRadarHit(SAMPLE_2);

        assertEquals(
           NopMessageType.MEARTS_RADAR_HIT,
            instance.getNopType()
        );

        assertEquals(
            instance.rawMessage(),
            SAMPLE_2
        );

        assertEquals(
            "ZUA",
            instance.facility()
        );
        assertEquals(
            1572967802000L + 20L,//computed at:  http://www.epochconverter.com/
            instance.time().toEpochMilli()
        );

        assertEquals(
            null,
            instance.callSign()
        );
        assertEquals(
            null,
            instance.aircraftType()
        );
        assertEquals(
            null,
            instance.equipmentTypeSuffix()
        );
        assertEquals(
            "2000",
            instance.reportedBeaconCode()
        );
        assertThat(instance.altitudeInHundredsOfFeet(), is(390));
        assertThat(instance.speed(), is(455.0));
        assertThat(instance.heading(), is(142.0));
        assertThat(instance.altitudeInHundredsOfFeet(), is(390));
        assertThat(instance.latitude(), is(016.1706));
        assertThat(instance.longitude(), is(149.0943));
        assertThat(instance.x(), is(499.43));
        assertThat(instance.y(), is(417.5876));

        assertEquals(
            "/",
            instance.controllingFacilitySector()
        );
        assertEquals(
            "ZUA_B",
            instance.sensorIdLetters()
        );
        assertEquals(
            null,
            instance.arrivalAirport()
        );
        assertEquals(
            null,
            instance.flightRules()
        );
        assertEquals(
            null,
            instance.departureAirport()
        );
        assertEquals(
            null,
            instance.eta()
        );
        assertEquals(
            "390//",
            instance.reportInterimAssignAltitude()
        );
        assertEquals(
            null,
            instance.heavyLargeOrSmall()
        );
        assertEquals(
            true,
            instance.onActiveSensor()
        );
    }


    @Test
    public void accessorMethodAlwaysReturnTheSameThing() {

       MeartsRadarHit meartsRh = (MeartsRadarHit)NopMessageType.parse(SAMPLE_1);

        /*
         * Equality using == is intended.
         *
         * Accessing the same field from the same RHMessage twice should yield the EXACT SAME string
         *
         * This test exists because these String values are parsed lazily. The parsing process used
         * to produce a new instance of the output String each time, this wasted memory.
         */
        assertSame(meartsRh.rawMessage(), meartsRh.rawMessage());
        assertSame(meartsRh.callSign(), meartsRh.callSign());
        assertSame(meartsRh.facility(), meartsRh.facility());
        assertSame(meartsRh.callSign(), meartsRh.callSign());
        assertSame(meartsRh.aircraftType(), meartsRh.aircraftType());
        assertSame(meartsRh.reportedBeaconCode(), meartsRh.reportedBeaconCode());
        assertSame(meartsRh.controllingFacilitySector(), meartsRh.controllingFacilitySector());
        assertSame(meartsRh.sensorIdLetters(), meartsRh.sensorIdLetters());
        assertSame(meartsRh.arrivalAirport(), meartsRh.arrivalAirport());
        assertSame(meartsRh.flightRules(), meartsRh.flightRules());
        assertSame(meartsRh.departureAirport(), meartsRh.departureAirport());
        assertSame(meartsRh.eta(), meartsRh.eta());
        assertSame(meartsRh.equipmentTypeSuffix(), meartsRh.equipmentTypeSuffix());
        assertSame(meartsRh.reportInterimAssignAltitude(), meartsRh.reportInterimAssignAltitude());
        assertSame(meartsRh.onActiveSensor(), meartsRh.onActiveSensor());
        assertSame(meartsRh.heavyLargeOrSmall(), meartsRh.heavyLargeOrSmall());
    }

    @Test
    public void accessorMethodsShouldUseFlyweightPattern() {

       MeartsRadarHit meartsRh1 = (MeartsRadarHit)NopMessageType.parse(SAMPLE_1);
       MeartsRadarHit meartsRh2 = (MeartsRadarHit) NopMessageType.parse(SAMPLE_2);

        /*
         * Equality using == is intended.
         *
         * Ensure accessing an equal field from two RHs messages yeilds the EXACT same string when
         * appropriate.
         *
         * i.e. Don't waste memory putting the many different copies of common Strings like "IFR",
         * "VFR", airport codes, and facility names into memory.
         */
        assertSame(meartsRh1.flightRules(), meartsRh1.flightRules());
        assertSame(meartsRh2.onActiveSensor(), meartsRh2.onActiveSensor());
    }


}
