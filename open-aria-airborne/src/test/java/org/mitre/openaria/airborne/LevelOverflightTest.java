
package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;

public class LevelOverflightTest {

    TrackPair testPair = makeTrackPairFromNopData(
        getResourceFile("ZFW--level-overflight.txt")
    );

    @Test
    public void runExample() {

        ConsumingArrayList<AirborneEvent> consumer = newConsumingArrayList();

        AirbornePairConsumer airborne = new AirbornePairConsumer(
            airborneAria(airborneProperties()),
            consumer
        );

        airborne.accept(testPair);

        //this event should have a high score (not a score like 17.5)
        assertThat(consumer.get(0).score(), greaterThan(80.0));
    }

    /* Use HUGE max reportable score to ensure an event is detected */
    static AirborneAlgorithmDef airborneProperties() {
        return defaultBuilder().maxReportableScore(10000.0).build();
    }

}
