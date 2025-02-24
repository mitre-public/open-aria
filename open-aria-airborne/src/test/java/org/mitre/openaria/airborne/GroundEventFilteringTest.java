
package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.airborne.AirborneTestUtils.confirmNoAirborneEventsAreDetected;

import java.util.List;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.threading.TrackMaking;

import org.junit.jupiter.api.Test;

public class GroundEventFilteringTest {

    /*
     * Each of these files contains two tracks that triggered a RiskMetric when one of the aircraft
     * is on the ground. The goal of this test is to ensure that examples like these do not generate
     * risk metric events.
     */
    static String[] examplesOfBuggedBehavior = {
        "groundEvent_example1.txt",
        "groundEvent_example2.txt",
        "groundEvent_example3.txt",
        "groundEvent_example4.txt",
//        "groundEvent_example5.txt",   //@todo -- find out why this one back in result set
//		"groundEvent_example6.txt",  //This event should be caught
        "groundEvent_example7.txt",
        "groundEvent_example8.txt",
        "groundEvent_example9.txt",
//        "groundEvent_example10.txt"  //@todo -- find out why this one back in result set
    };

    @Test
    public void testFilteringOutGroundEvents() {

        for (String fileName : examplesOfBuggedBehavior) {

            TrackPair trackPair = TrackMaking.makeTrackPairFromNopData(getResourceFile(fileName));

            confirmNoAirborneEventsAreDetected(trackPair);
        }
    }


    @Test
    public void slowMovingButAloftDataIsNotRemoved() {
        /*
         * At NOP facility APA on 8/27/21 @ 1321 UTR the ARIA program missed an encounter between
         * N320LX and a 1200 code aircraft. The event was missed because the 1200 code aircraft took
         * off while moving very slowly, and the slow moving track data was removed.
         */
        String sampleFile = "missingAPA.txt";

        TrackPair trackPair = TrackMaking.makeTrackPairFromNopData(getResourceFile(sampleFile));

        List<AirborneEvent> results = processTrack(trackPair, 25.0);

        assertThat(results, not(empty()));

        //Event Details:
        //    "timestamp": "2021-08-27T13:21:12.701Z",
        //    "epochMsTime": 1630070472701,
        //    "score": 20.91859,
        //    "trueVerticalFt": 157.42661,
        //    "trueLateralNm": 0.45391,
        //    "estTimeToCpaMs": 10658,
        //    "estVerticalAtCpaFt": 622.68908,
        //    "estLateralAtCpaNm": 0.12504
    }

    public List<AirborneEvent> processTrack(TrackPair pair, double maxScore) {

        AirborneAlgorithmDef def = defaultBuilder()
            .maxReportableScore(maxScore)
            .build();

        AirborneAria tpp = airborneAria(def);

        //create a consumer that collects all the number RiskMetricEventSummary objects it receives
        ConsumingArrayList<AirborneEvent> foundEvents = newConsumingArrayList();

        //set up a RiskMetricTrackPairConsumer that pipes event summaries to the above aggegator
        AirbornePairConsumer consumer = new AirbornePairConsumer(
            tpp,
            foundEvents
        );

        consumer.accept(pair);

        return foundEvents; //ConsumingArrayList extends ArrayList
    }
}
