package org.mitre.openaria.core.formats.swim;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.formats.swim.SwimStddsMessageUnmarshaller.newTaisMessageUnmarshaller;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mitre.swim.parse.JaxbSwimMessage;

import org.junit.jupiter.api.Test;

// instantiating the unmarshaller for the SWIM parsers is high overhead
class TestSwimTaisR40PointParser {

    private static final Function<String, Optional<JaxbSwimMessage>> unmarshaller = newTaisMessageUnmarshaller();

    private static final SwimTaisR40PointParser parser = new SwimTaisR40PointParser();

    @Test
    void testParseSampleR40Message() {
        Map<Integer, TaisPoint> points = unmarshaller.apply(SampleTaisMessages.sampleR40TrackFlightplan)
            .map(parser).orElseThrow(IllegalStateException::new)
            .stream()
            .collect(Collectors.toMap(TaisPoint::trackNum, Function.identity()));

        assertAll(
            "TAIS point field population assertions",

            // Track message parsed fields
            () -> assertEquals(3196, points.get(3196).trackNum(), "Track Number"),
            () -> assertEquals(Instant.parse("2019-12-31T00:00:00.689Z"), points.get(3196).time(), "Time"),
            () -> assertEquals("a3be94", points.get(3196).acAddress(), "AC Address"),
            () -> assertEquals(Optional.of(TaisPoint.TrackStatusType.ACTIVE), points.get(3196).status(), "Track Status"),
            () -> assertEquals(922257 * SwimTaisR40PointParser.POSITION_RESOLUTION, points.get(3196).xpos(), .1, "X Position"),
            () -> assertEquals(310004 * SwimTaisR40PointParser.POSITION_RESOLUTION, points.get(3196).ypos(), .1, "Y Position"),
            () -> assertEquals(105, points.get(3196).vx(), "VX"),
            () -> assertEquals(59, points.get(3196).vy(), "VY"),
            () -> assertEquals((short) 1, points.get(3196).adsb(), "ADS-B"),
            () -> assertEquals((short) 0, points.get(3196).frozen(), "Frozen"),
            () -> assertEquals((short) 0, points.get(3196).newProp(), "New"),
            () -> assertEquals((short) 0, points.get(3196).pseudo(), "Pseudo"),
            () -> assertEquals("1773", points.get(3196).reportedBeaconCode(), "Beacon Code"),
            () -> assertEquals(5700., points.get(3196).reportedAltitude(), "Reported Altitude"),

            // Flight plan message parsed fields
            () -> assertEquals("KT79448200", points.get(3196).eramGufi(), "ERAM GUFI"),
            () -> assertEquals("us.fdps.2019-12-30T22:04:08Z.000/19/200", points.get(3196).sfdpsGufi(), "SFDPS GUFI"),
            () -> assertNull(points.get(3196).scratchpad1(), "Scratchpad 1"),
            () -> assertNull(points.get(3196).scratchpad2(), "Scratchpad 2"),
            () -> assertEquals("N340PA", points.get(3196).callsign(), "Callsign"),
            () -> assertEquals("PA28", points.get(3196).aircraftType(), "Aircraft Type"),
            () -> assertEquals("V", points.get(3196).flightRules(), "Flight Rules"),
            () -> assertNull(points.get(3196).keyboard(), "Keyboard"),
            () -> assertNull(points.get(3196).positionSymbol(), "Position Symbol"),

            // these aren't entirely correct - but its an error in TAIS itself...
            () -> assertEquals("KINT", points.get(3196).arrAirport(), "Arrival Airport"),
            () -> assertEquals("RMG291047", points.get(3196).depAirport(), "Departure Airport")
        );
    }
}
