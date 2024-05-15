package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;
import java.util.ArrayList;

import org.mitre.openaria.core.TrackPair;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class LevelOffEventTest {

    private static AirborneEvent applyKpi(AirborneAria tpp, TrackPair input) {

        ArrayList<AirborneEvent> results = newArrayList(
            tpp.findAirborneEvents(input)
        );

        assertThat(results, Matchers.hasSize(1));

        return results.get(0);
    }

    @Test
    public void canFindLevelOffEvent() {
        TrackPair levelOffEvent = makeTrackPairFromNopData(
            new File("src/test/resources/levelOffEvent.txt"));

        AirborneEvent event = applyKpi(airborneAria(), levelOffEvent);
        assertThat(event.isLevelOffEvent(), is(true));
    }

    @Test
    public void canFindLevelOffEvents_example2() {
        TrackPair almostLevelOffEvent = makeTrackPairFromNopData(
            getResourceFile("almostLevelOffEvent_didNotLevelInTime.txt")
        );
        AirborneEvent event = applyKpi(airborneAria(), almostLevelOffEvent);

        assertThat(event.isLevelOffEvent(), is(false));
    }

    @Test
    public void notEverythingIsALevelOffEvent() {

        AirborneEvent event = applyKpi(
            airborneAria(),
            makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"))
        );

        assertThat(event.isLevelOffEvent(), is(false));
    }
}
