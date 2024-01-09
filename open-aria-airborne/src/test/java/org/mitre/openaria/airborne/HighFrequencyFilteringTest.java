
package org.mitre.openaria.airborne;

import static org.mitre.openaria.airborne.AirborneTestUtils.confirmNoAirborneEventsAreDetected;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;

public class HighFrequencyFilteringTest {

    /*
     * Each of these files contains bad data in which multiple radar hits occur in too small a
     * window of time.
     *
     * These example are from RAW data. Something must be done to "smooth out" or "remove" this type
     * of input data. You cannot output tons
     */
    static String[] examplesOfBuggedBehavior = {
        //this example should not produce an event (because it is 1 flight)
        "highFrequncyPoints_example1.txt",
        //this example may produce an event, given your tolerances (the 2nd aircraft is about 1.6 miles in trail)
        "highFrequncyPoints_example2.txt"
    };

    @Test
    public void testTracksWithBadData() {

        TrackPair trackPair = makeTrackPairFromNopData(getResourceFile(examplesOfBuggedBehavior[0]));
        confirmNoAirborneEventsAreDetected(trackPair);
    }

}
