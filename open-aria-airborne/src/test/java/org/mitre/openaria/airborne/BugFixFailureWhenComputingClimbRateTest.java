
package org.mitre.openaria.airborne;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;

public class BugFixFailureWhenComputingClimbRateTest {

    private TrackPair getFailingTrackPair() {
        /*
         * processing this data failed because the "compute climb rate" function looked for data off
         * the end of the track where there was no data.
         */
        return makeTrackPairFromNopData(getResourceFile("bug_failWhileComputingClimbRate.txt"));
    }

    /* Processing this track should not throw an exception. */
    @Test
    public void testRiskMetricOnDataWithFlawedAltitudeValues() {
        //create a consumer that collects all the number RiskMetricEventSummary objects it receives
        ConsumingArrayList<AirborneEvent> foundEvents = newConsumingArrayList();

        //setup a RiskMetricTrackPairConsumer that pipes event summaries to the above aggegator
        AirbornePairConsumer consumer = new AirbornePairConsumer(
            airborneAria(testProps()),
            foundEvents
        );
        consumer.accept(getFailingTrackPair());
        /*
         * The original failure happend when calling: computeBothClimbRates() when trying to create
         * an EventRecord at a time stamp around 00:21:47.500
         */
        assertThat(foundEvents, not(empty()));
    }

    private static AirborneAlgorithmDef testProps() {
        //use a very large max so that we ALWAYS get an event
        return defaultBuilder().maxReportableScore(10000.0).build();
    }
}
