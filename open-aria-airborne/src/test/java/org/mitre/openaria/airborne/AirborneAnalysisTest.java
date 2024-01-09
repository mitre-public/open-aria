package org.mitre.openaria.airborne;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.Distance;

public class AirborneAnalysisTest {

    static final TrackPair SCARY_TRACK_PAIR = scaryTrackPair();

    //store in a static field so we don't recompute this guy multiple times (help build speed)
    private static TrackPair scaryTrackPair() {

        TrackPair raw = makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));
        TrackPair smoothed = airborneAria().dataCleaner().clean(raw).get();  //smooth data to fill in gaps

        return smoothed;
    }

    @Test
    public void canMakeAirborneAnalysis() {
        AirborneAnalysis aa = new AirborneAnalysis(SCARY_TRACK_PAIR);

        Distance maxLateralSep = Stream.of(aa.trueLateralSeparations())
            .max(Comparator.naturalOrder())
            .get();

        Duration actualDuration = aa.timeWindow().duration();

        assertThat(actualDuration, greaterThan(Duration.ofSeconds(9 * 60 + 36)));   //9m 36s
        assertThat(maxLateralSep, greaterThan(Distance.ofNauticalMiles(14.0)));
    }


    @Test
    public void canTruncatedAirborneAnalysis() {

        AirborneAnalysis truncated = (new AirborneAnalysis(SCARY_TRACK_PAIR))
            .truncate(Distance.ofNauticalMiles(5));

        Distance maxLateralSep = Stream.of(truncated.trueLateralSeparations())
            .max(Comparator.naturalOrder())
            .get();

        Duration actualDuration = truncated.timeWindow().duration();

        assertThat(actualDuration, lessThan(Duration.ofSeconds(6 * 60 + 16)));  //6m 16s (down from 9m 36s)
        assertThat(maxLateralSep, lessThan(Distance.ofNauticalMiles(5))); // 5nm (down from 14.5 nm)
    }
}