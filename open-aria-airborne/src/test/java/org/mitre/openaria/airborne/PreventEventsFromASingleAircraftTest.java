
package org.mitre.openaria.airborne;

import static org.mitre.openaria.airborne.AirborneTestUtils.confirmNoAirborneEventsAreDetected;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;

/**
 * The purpose of this set of test is to confirm that FINSIH ME !!!
 */
public class PreventEventsFromASingleAircraftTest {

    /*
     * This bug occurs when two different tracks from a Center's NOP feed track the same flight for
     * a little while. Basically, the duplicate data causes Risk Metric events to get generated "by
     * a single track and a copy of itself".
     *
     * The bookmarkNumbers shown above are examples of this problem.
     */
    @Test
    public void bug111_riskMetricEventsFromASingleTrack() {

        TrackPair trackPair = makeTrackPairFromNopData(getResourceFile("oneFlightTwoBeaconCodes.txt"));

        confirmNoAirborneEventsAreDetected(trackPair);
    }

}
