package org.mitre.openaria.threading.demo;

import static org.mitre.openaria.smoothing.TrackSmoothing.coreSmoothing;

import java.io.File;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.util.SingleUseTimer;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.StreamingTimeSorter;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.NopParser;
import org.mitre.openaria.threading.TrackMaker;

import com.google.common.math.StatsAccumulator;


/**
 * Contains a short program that smooths one day's worth of NOP data for a single facility. The goal
 * of the program is to (1) show how to do this and (2) measure the execution speed.
 */
public class ProfilingDemo {

    /**
     * Process a few samples of NOP data to produce a rough-order-of-magnitude approximation of how
     * long it takes to smooth data using "coreSmoothing"
     *
     * <p>Test inputs:
     * C90 data = 117.4 MB, 2,662,506 points
     * A80 data = 89.7 MB, 1,964,449 points
     * ZTL data = 72.7 MB, 1,595,276 points
     *
     * <p>Test output:
     * C90 = 2m 21 sec
     * A80 = 1m 51 sec
     * ZTL = 1m 5 sec
     */
    public static void main(String[] args) {

        //downloaded from: hdp1-gw01:/data/cda/raw/nop/archive/C90/2021/05/30/.gz
        String[] sourceFiles = new String[] {
            "/Users/jiparker/rawData/data-from-2021-05-30/STARS_C90_RH_20210530.txt.gz",
            "/Users/jiparker/rawData/data-from-2021-05-30/STARS_A80_RH_20210530.txt.gz",
            "/Users/jiparker/rawData/data-from-2021-05-30/CENTER_ZTL_RH_20210530.txt.gz"
        };

        for (String source : sourceFiles) {
            timeTrackSmoothing(new File(source));
        }
    }


    private static void timeTrackSmoothing(File sourceFile) {

        System.out.println("\nInput: " + sourceFile.getName());
        System.out.println(" Size: " + sourceFile.length());

        PointIterator iterator = new PointIterator(new NopParser(sourceFile));

        //This TrackMaker will smooth every Track it emits..
        Smoother smoother = new Smoother();
        StreamingTimeSorter<Point> sorter2 = new StreamingTimeSorter<>(
            new TrackMaker(smoother),
            Duration.ofMinutes(5)
        );

        SingleUseTimer timer = new SingleUseTimer();
        timer.tic();

        //step through the source data file...generate Tracks and Smooth them as you go
        iterator.forEachRemaining(sorter2);
        sorter2.flush();

        timer.toc();

        System.out.println("Elapsed Time: " + timer.elapsedTime().toString());
        smoother.summarizeTrackPointCounts();
    }

    //Receives Tracks, smooths those track using legacy smoothing and keeps track of point count
    static class Smoother implements Consumer<Track> {

        StatsAccumulator stats = new StatsAccumulator();
        DataCleaner<Track> smoother = coreSmoothing();

        @Override
        public void accept(Track track) {
            //Do the computation work...
            Optional<Track> smoothedTrack = smoother.clean(track);

            //Remember how much work was done (under estimates count because small tracks are thrown out)
            int pointCount = smoothedTrack.map(Track::size).orElse(0);
            stats.add(pointCount);
        }

        private void summarizeTrackPointCounts() {

            System.out.println("Total Num Points : " + (stats.count() * stats.mean()));
            System.out.println("Metrics for Num RH Messages per Track:");
            System.out.println("  Min   : " + stats.min());
            System.out.println("  Mean  : " + stats.mean());
            System.out.println("  Max   : " + stats.max());
            System.out.println("  StdDev: " + stats.sampleStandardDeviation());
        }
    }
}
