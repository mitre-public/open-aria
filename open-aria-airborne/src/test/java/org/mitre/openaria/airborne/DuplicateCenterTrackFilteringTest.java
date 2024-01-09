
package org.mitre.openaria.airborne;

import static org.mitre.openaria.airborne.AirborneTestUtils.confirmNoAirborneEventsAreDetected;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;

/**
 * The purpose of this testing suite is to confirm that efforts to filter out duplicate Track
 * Sections from Risk Metric are successful.
 */
public class DuplicateCenterTrackFilteringTest {

    /*
     * Each of these files contains two tracks that triggered a RiskMetric event against a single
     * aircraft and itself. Each pair of tracks contains a section of duplicate data. The goal of
     * this test is to ensure that examples like these do not generate risk metric events because
     * these "track pairs" are actually just a single track.
     */
    static String[] examplesOfBuggedBehavior = {
        "duplicateCenterData_example1.txt",
        "duplicateCenterData_example2.txt",
        "duplicateCenterData_example3.txt",
        "duplicateCenterData_example4.txt",
        "duplicateCenterData_example5.txt"
    };

    @Test
    public void bug61_riskMetricEventsFromASingleTrack() {

        for (String fileName : examplesOfBuggedBehavior) {

            TrackPair trackPair = makeTrackPairFromNopData(getResourceFile(fileName));

            confirmNoAirborneEventsAreDetected(trackPair);
        }
    }

}
