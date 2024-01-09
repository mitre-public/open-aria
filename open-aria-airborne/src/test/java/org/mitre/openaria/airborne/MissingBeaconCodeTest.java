
package org.mitre.openaria.airborne;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.threading.TrackMaking;

public class MissingBeaconCodeTest {

    /**
     * The track data used in this test caused an low-probability Exception during an "at-scale
     * test". This test ensures that we can make EventRecords when the source data does not have
     * beacon code information.
     */
    @Test
    public void canMakeEventRecordWhenTrackDataIsMissingBeaconCodes() {

        Instant eventTime = Instant.ofEpochMilli(1476830107500L);

        TrackPair eventWithoutBeacon = TrackMaking.makeTrackPairFromNopData(
            getResourceFile("eventWithoutBeaconCode.txt")
        );

        double arbitraryScore = 5.0;
        ScoredInstant si = new ScoredInstant(arbitraryScore, eventTime);

        AirborneEvent airborneEvent = new AirborneEvent(eventWithoutBeacon, si);

        assertThat(airborneEvent.firstAircraft().beaconCode(), is("UNKNOWN"));
        assertThat(airborneEvent.secondAircraft().beaconCode(), is("1462"));
    }
}
