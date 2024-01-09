
package org.mitre.openaria;

import static org.mitre.openaria.pointpairing.PairingConfig.standardPairingProperties;

import java.io.File;

import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.trackpairing.TrackPairer;
import org.mitre.caasd.commons.CountingConsumer;
import org.mitre.caasd.commons.parsing.nop.NopParser;
import org.mitre.caasd.commons.util.SingleUseTimer;

/**
 * The purpose of this program is to Benchmark the TrackPairing code.
 * <p>
 * Measuring this step is particularly important now that the Track Pairing distance has been
 * increased to 10 NM (up from from 1 NM).
 */
public class BenchmarkTrackPairing {

    private static final String NOP_DATA_FILE = "/Users/jiparker/rawData/cleanFaaNopDataSample/A80_rhMessages.gz";

    public static void main(String[] args) {
        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();
        processData(new File(NOP_DATA_FILE));
        timer.toc();
        System.out.println("Time to process file = " + timer.elapsedTime().getSeconds() + " seconds");
    }

    private static void processData(File file) {

        CountingConsumer<TrackPair> pairCounter = new CountingConsumer<>(
            (TrackPair pair) -> {
                //do nothing with each TrackPair, just allow the CountingConsumer to count
            }
        );

        TrackPairer trackPairer = new TrackPairer(
            pairCounter,
            standardPairingProperties()
        );

        NopParser parser = new NopParser(file);
        PointIterator pointIter = new PointIterator(parser);

        int pointCounter = 0;

        while (pointIter.hasNext()) {
            trackPairer.accept(pointIter.next());
            pointCounter++;

            if (pointCounter % 10_000 == 0) {
                System.out.println("Point count: " + pointCounter + "\tPair count: " + pairCounter.numCallsToAccept());
            }
        }

        System.out.println("Final Point count: " + pointCounter + "\tFinal Pair count: " + pairCounter.numCallsToAccept());
    }
}
