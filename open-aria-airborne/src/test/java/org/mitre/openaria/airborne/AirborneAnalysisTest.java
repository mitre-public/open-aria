package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.util.Comparator;
import java.util.stream.Stream;

import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.TrackPair;

import org.junit.jupiter.api.Test;

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

        assertThat(maxLateralSep, greaterThan(Distance.ofNauticalMiles(14.0)));
    }


    @Test
    public void canTruncatedAirborneAnalysis() {

        AirborneAnalysis truncated = (new AirborneAnalysis(SCARY_TRACK_PAIR))
            .truncate(Distance.ofNauticalMiles(5));

        Distance maxLateralSep = Stream.of(truncated.trueLateralSeparations())
            .max(Comparator.naturalOrder())
            .get();

        assertThat(maxLateralSep, lessThan(Distance.ofNauticalMiles(5))); // 5nm (down from 14.5 nm)
    }
}