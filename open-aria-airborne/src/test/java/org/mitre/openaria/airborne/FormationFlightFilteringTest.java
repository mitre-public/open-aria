
package org.mitre.openaria.airborne;

import static org.mitre.openaria.airborne.AirborneTestUtils.confirmNoAirborneEventsAreDetected;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.threading.TrackMaking;

public class FormationFlightFilteringTest {

    /*
     * Each of these files contains two tracks that triggered a RiskMetric event against two
     * aircraft that are clearly intending to fly close to one another. The goal of this test is to
     * ensure that examples like these do not generate risk metric events.
     */
    static String[] examplesOfBuggedBehavior = {
        "formationFlight_example1.txt"
//		,"formationFlight_example2.txt" //its unclear if this track should create a "real warning"
    };

    @Disabled // This formation flight SHOULD be "removable"...with the correct configuration.....that configuration is hard coded for now :(
    @Test
    public void bug61_riskMetricEventsFromASingleTrack() {

        for (String fileName : examplesOfBuggedBehavior) {

            TrackPair trackPair = TrackMaking.makeTrackPairFromNopData(getResourceFile(fileName));

            confirmNoAirborneEventsAreDetected(trackPair);
        }
    }

}
