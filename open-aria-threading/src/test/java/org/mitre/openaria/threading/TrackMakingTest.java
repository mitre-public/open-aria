
package org.mitre.openaria.threading;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;

import org.mitre.openaria.core.TrackPair;

import org.junit.jupiter.api.Test;

public class TrackMakingTest {

    @Test
    public void testMakeTrackPairFromNopData() {

        File testData = getResourceFile(
            "org/mitre/openaria/threading/MEARTS-11-05-19-trackData.txt"
        );
//        File testData = new File("src/test/resources/org/mitre/openaria/threading/MEARTS-11-05-19-trackData.txt");

        TrackPair pair = makeTrackPairFromNopData(testData);

        assertNotNull(pair);
        assertNotEquals(pair.track1(), pair.track2());

        assertTrue(pair.track1().trackId().equals("236"));
        assertTrue(pair.track2().trackId().equals("204"));

        assertTrue(pair.track1().aircraftType().equals("B77W"));
        assertTrue(pair.track2().aircraftType().equals("A321"));


    }
}
