
package org.mitre.openaria.airborne;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.AirborneUtils.isEstablishedAtAltitude;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseNopTime;
import static org.mitre.openaria.threading.TrackMaking.makeTrackFromNopData;

import java.time.Duration;
import java.time.Instant;

import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class AirborneUtilsTest {

    @Test
    public void isEstablishedAtAltitude_providesCorrectAnswer() {

        Track track = makeTrackFromNopData(getResourceFile("Track2.txt"));

        Duration fiveSeconds = Duration.ofSeconds(5);

        assertThat(
            isEstablishedAtAltitude(track, track.startTime(), fiveSeconds),
            is(false)
        );

        //first time at 8,000ft
        Instant timeAt8000 = parseNopTime("07/08/2017", "14:23:13.220");

        //when you JUST hit 8,000ft you ARE established if you require 31 seconds (because you'll only have 7900 and 8000 foot altitudes)
        assertThat(
            isEstablishedAtAltitude(track, timeAt8000, Duration.ofSeconds(31)),
            is(true)
        );
        //when you JUST hit 8,000ft you ARE NOT established if you require 32 seconds (because you'll have 7800, 7900, and 8000 foot altitudes)
        assertThat(
            isEstablishedAtAltitude(track, timeAt8000, Duration.ofSeconds(32)),
            is(false)
        );

        //2nd point at 8,000ft
        Instant timeAt8000_2 = parseNopTime("07/08/2017", "14:23:17.249");

        //you are established at an altitude after the 2nd point
        assertThat(
            isEstablishedAtAltitude(track, timeAt8000_2, fiveSeconds),
            is(true)
        );

        //another point at 8,000ft
        Instant timeAt8000_4 = parseNopTime("07/08/2017", "14:23:24.147");
        assertThat(
            isEstablishedAtAltitude(track, timeAt8000_4, fiveSeconds),
            is(true)
        );

        //you can make the test above fail by requiring a long duration at the same altitude
        assertThat(
            isEstablishedAtAltitude(track, timeAt8000_4, Duration.ofMinutes(10)),
            is(false)
        );

        Instant timeAt7900 = parseNopTime("07/08/2017", "14:36:40.981");

        //you'll still be established at the altitude even though dropped from 8,000ft to 7,900ft.
        assertThat(
            isEstablishedAtAltitude(track, timeAt7900, Duration.ofSeconds(30)),
            is(true)
        );
    }

    @Test
    public void testIsEstablishedAtAltitude_outOfBoundsQueriesDoNotGenerateExceptions() {

        Track track = makeTrackFromNopData(getResourceFile("Track2.txt"));

        Duration fiveSeconds = Duration.ofSeconds(5);

        Instant timeBeforeTrackExists = track.startTime().minusSeconds(1_000);

        assertDoesNotThrow(
            () ->  isEstablishedAtAltitude(track, timeBeforeTrackExists, fiveSeconds)
        );
    }
}
