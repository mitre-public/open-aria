
package org.mitre.openaria.airborne;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.SeparationPrediction.format;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.time.Duration;
import java.time.Instant;

import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Test;

public class SeparationPredictionTest {

    @Test
    public void testProjectedVerticalSeparationInRiskMetric() {
        /*
         * The two TrackPairs are initially identical, but their altitude separations change at a
         * later time. If the score correctly depends on the projected vertical separation at the
         * projected time of CPA, then the two tracks will have the same score at the start time. If
         * the score incorrectly depends on the actual vertical separation at the projected time of
         * CPA, then the two tracks will have different scores at the start time because the actual
         * vertical separation is different there.
         */

        TrackPair<?> trackPair1 = getTrackPair1();
        TrackPair<?> trackPair2 = getTrackPair2();

        Instant startTime = trackPair1.timeOverlap().get().start();

        SeparationPrediction prediction1 = new SeparationPrediction(trackPair1, startTime);
        SeparationPrediction prediction2 = new SeparationPrediction(trackPair2, startTime);

        assertThat(
            prediction1.verticalSeparationAtCpa,
            equalTo(prediction2.verticalSeparationAtCpa)
        );
    }

    private TrackPair<NopHit> getTrackPair1() {
        return makeTrackPairFromNopData(getResourceFile("verticalSeparationTrackData1.txt"));
    }

    private TrackPair getTrackPair2() {
        return makeTrackPairFromNopData(getResourceFile("verticalSeparationTrackData2.txt"));
    }

    @Test
    public void durationFormatOnShortTimesWorksDoesNotIncludeMinsOrHours() {

        Duration fiveSec = Duration.ofMillis(5_123);

        assertThat(format(fiveSec), equalTo("5.123 sec"));
    }

    @Test
    public void durationFormatOnShortTimesWorks() {

        Duration oneMinfiveSec = Duration.ofMillis(5_123).plusMinutes(1);

        assertThat(format(oneMinfiveSec), equalTo("1m 5.123 sec"));
    }
}
