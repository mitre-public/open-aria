
package org.mitre.openaria.threading;

import static com.google.common.base.Preconditions.*;
import static java.util.Collections.sort;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.openaria.core.ApproximateTimeSorter;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParser;

import com.google.common.collect.Lists;

public class TrackMaking {

    /**
     * Create one TrackPair from a text file that contains raw NOP data from exactly two Tracks.
     *
     * @param nopFile A text file containing raw NOP data from exactly two tracks.
     *
     * @return A TrackPair
     */
    public static TrackPair makeTrackPairFromNopData(File nopFile) {
        try {
            return TrackPair.from(makeTracksFromNopData(nopFile));
        } catch (IllegalArgumentException iae) {
            //throw a better exception when too many tracks are found
            throw new RuntimeException(nopFile.getAbsolutePath() + " caused an exception", iae);
        }
    }

    public static Track<NopHit> makeTrackFromNopData(File nopFile) {
        Collection<Track<NopHit>> tracks = makeTracksFromNopData(nopFile);
        checkState(tracks.size() == 1, "Expected exactly 1 track, found: " + tracks.size());

        return tracks.iterator().next();
    }

    /**
     * Apply the
     *
     * @param nopFile
     *
     * @return
     */
    public static ConsumingArrayList<Track<NopHit>> makeTracksFromNopData(File nopFile) {

        checkNotNull(nopFile, "The input file cannot be null");
        checkArgument(nopFile.exists(), "Input file (" + nopFile.getName() + ") does not exist");
        checkArgument(!nopFile.isDirectory(), "Input file (" + nopFile.getName() + ") should not be a directory");

        NopParser parser = new NopParser(nopFile);
        PointIterator pointIter = new PointIterator(parser);
        List<Point<NopHit>> points = Lists.newArrayList(pointIter);

        sort(points);

        return extractTracks(points.iterator());
    }

    /**
     * @param pointIter A source of Point data.
     *
     * @return The Collection of Track found by using a TrackMaker to assemble Tracks from Points
     */
    public static <T> ConsumingArrayList<Track<T>> extractTracks(Iterator<Point<T>> pointIter) {

        //this aggregator collects all tracks the TrackMaker builds
        ConsumingArrayList<Track<T>> aggregator = newConsumingArrayList();

        TrackMaker<T> maker = new TrackMaker<>(aggregator);

        ApproximateTimeSorter<Point<T>> timeSorter = new ApproximateTimeSorter<>(
            Duration.ofMinutes(10),
            maker
        );

        //walk the input data...push all data to the TrackMaker
        while (pointIter.hasNext()) {
            timeSorter.accept(pointIter.next());
        }
        //make sure no data is stranded either in the time sorter or the track maker
        timeSorter.flush();
        maker.flushAllTracks();
        return aggregator;
    }

}
