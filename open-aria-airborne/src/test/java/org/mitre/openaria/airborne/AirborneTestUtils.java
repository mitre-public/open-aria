
package org.mitre.openaria.airborne;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopEncoder;

public class AirborneTestUtils {

    /**
     * Process this TrackPair, confirm that no Airborne events are detected.
     *
     * @param pair A test TrackPair
     */
    public static void confirmNoAirborneEventsAreDetected(TrackPair pair) {

        AirborneAlgorithmDef standardConfig = new AirborneAlgorithmDef();

        AirborneAria tpp = airborneAria(standardConfig);

        //create a consumer that collects all the number RiskMetricEventSummary objects it receives
        ConsumingArrayList<AirborneEvent> foundEvents = newConsumingArrayList();

        //setup a RiskMetricTrackPairConsumer that pipes event summaries to the above aggegator
        AirbornePairConsumer consumer = new AirbornePairConsumer(
            tpp,
            foundEvents
        );

        consumer.accept(pair);

        if (!foundEvents.isEmpty()) {
            System.out.println("FAILED: Found an event when we shouldn't have");

            System.out.println(foundEvents.get(0).asJson());

            NopEncoder nopEncoder = new NopEncoder();

            System.out.println(nopEncoder.asRawNop(pair.track1()));
            System.out.println("");
            System.out.println(nopEncoder.asRawNop(pair.track2()));
        }

        assertTrue(foundEvents.isEmpty(), "No event expected");
    }

}
