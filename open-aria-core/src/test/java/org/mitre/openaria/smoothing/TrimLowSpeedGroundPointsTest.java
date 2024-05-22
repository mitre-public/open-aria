package org.mitre.openaria.smoothing;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParsingUtils;

import org.junit.jupiter.api.Test;


public class TrimLowSpeedGroundPointsTest {

    /*
     * At NOP facility APA on 8/27/21 @ 1321 UTR the ARIA program missed an encounter between N320LX
     * and a 1200 code aircraft. The event was missed because the 1200 code aircraft took off while
     * moving very slowly, and the slow moving track data was removed.
     *
     * The data below was extracted from that event where the 1200 code aircaft too off at 6200ft
     * and 66knots. He jumps to 86 knots for one point, then the aircraft starts gaining altitude.
     * He gets to 7000ft (800AGL) before he reaching 80 knots.
     */
    String[] rawNopPoints = new String[]{
        "[RH],STARS,D01,08/27/2021,13:19:43.135,,,,1200,062,066,180,039.56437,-104.85220,3600,0000,76.3563,22.4852,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:19:47.627,,,,1200,062,086,161,039.56239,-104.85090,3600,0000,76.4188,22.3680,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:19:52.984,,,,1200,063,078,169,039.56063,-104.85077,3600,0000,76.4266,22.2625,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:19:57.846,,,,1200,063,075,171,039.55900,-104.85064,3600,0000,76.4344,22.1648,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:02.249,,,,1200,064,070,174,039.55783,-104.85067,3600,0000,76.4344,22.0945,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:06.195,,,,1200,064,070,175,039.55646,-104.85070,3600,0000,76.4344,22.0125,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:11.665,,,,1200,064,071,174,039.55470,-104.85040,3600,0000,76.4500,21.9070,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:15.512,,,,1200,065,071,174,039.55339,-104.85018,3600,0000,76.4617,21.8289,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:20.929,,,,1200,065,071,173,039.55163,-104.84989,3600,0000,76.4773,21.7234,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:25.025,,,,1200,065,071,173,039.55025,-104.84958,3600,0000,76.4930,21.6414,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:29.081,,,,1200,066,071,172,039.54888,-104.84936,3600,0000,76.5047,21.5594,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:33.186,,,,1200,066,072,172,039.54751,-104.84914,3600,0000,76.5164,21.4773,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:38.043,,,,1200,066,072,172,039.54588,-104.84876,3600,0000,76.5359,21.3797,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:43.208,,,,1200,067,072,172,039.54411,-104.84846,3600,0000,76.5516,21.2742,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:47.409,,,,1200,067,072,172,039.54274,-104.84824,3600,0000,76.5633,21.1922,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:51.617,,,,1200,067,072,172,039.54137,-104.84802,3600,0000,76.5750,21.1102,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:20:55.666,,,,1200,068,072,172,039.54000,-104.84789,3600,0000,76.5828,21.0281,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:00.274,,,,1200,068,071,172,039.53850,-104.84767,3600,0000,76.5945,20.9383,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:05.643,,,,1200,068,071,173,039.53680,-104.84746,3600,0000,76.6063,20.8367,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:10.609,,,,1200,069,069,176,039.53530,-104.84758,3600,0000,76.6023,20.7469,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:15.417,,,,1200,069,058,216,039.53468,-104.84936,3600,0000,76.5203,20.7078,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:20.184,,,,1200,069,059,245,039.53451,-104.85130,3600,0000,76.4305,20.6961,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:24.841,,,,1200,069,061,262,039.53447,-104.85307,3600,0000,76.3484,20.6922,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:28.846,,,,1200,069,064,270,039.53456,-104.85466,3600,0000,76.2742,20.6961,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:32.951,,,,1200,069,068,278,039.53491,-104.85642,3600,0000,76.1922,20.7156,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:36.996,,,,1200,070,069,289,039.53552,-104.85792,3600,0000,76.1219,20.7508,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}",
        "[RH],STARS,D01,08/27/2021,13:21:41.556,,,,1200,070,081,311,039.53691,-104.85966,3600,0000,76.0398,20.8328,,,,D01,,,,,,ACT,IFR,,00000,A2A0BB,,,,,,1,,0,{RH}"
    };

    public Track<NopHit> trackWithLowSpeedTakeOff() {
        List<Point<NopHit>> points = Stream.of(rawNopPoints)
            .map(str -> NopHit.from(str))
            .collect(toList());

        return Track.of(points);
    }

    @Test
    public void removeSlowMovingDataAtEndsOfTrack() {

        TrimSlowMovingPointsWithSimilarAltitudes<NopHit> smoother = new TrimSlowMovingPointsWithSimilarAltitudes<>(
            Speed.of(90, KNOTS),
            Distance.ofFeet(150),
            5
        );

        Track<NopHit> testTrack = trackWithLowSpeedTakeOff();

        Optional<Track<NopHit>> cleanedTrack = smoother.clean(testTrack);

        assertThat(cleanedTrack.isPresent(), is(true));
        Track<NopHit> track = cleanedTrack.get();
        int numRemovedFromFront = 4;
        int numRemovedFromBack = 8;
        assertThat(track.size(), is(27 - numRemovedFromFront - numRemovedFromBack));

        assertTrue(track.points().first().time().equals(
            NopParsingUtils.parseNopTime("08/27/2021", "13:20:02.249")
        ));
    }


}
