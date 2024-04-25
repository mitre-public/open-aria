package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static org.mitre.caasd.commons.util.PropertyUtils.getInt;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.StreamingTimeSorter;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.pointpairing.PairingConfig;
import org.mitre.openaria.pointpairing.PointPairFinder;
import org.mitre.openaria.threading.TrackMaker;
import org.mitre.openaria.trackpairing.TrackPairer;

/**
 * A StreamingKpi is a vehicle for deploying an Event Detection algorithm like Airborne ARIA,
 * Surface ARIA, or CFIT.
 *
 * <p>A StreamingKpi helps run an event detection algorithm (which is a Track or TrackPair based
 * piece of logic) by: (1) organizing a stream of raw Point data, (2) creating Tracks or TrackPairs
 * from that organized Point stream, and (3) passing the resulting Tracks or TrackPairs to the Event
 * Detection Algorithm "at the heart" of the StreamingKpi (which was supplied at construction).
 *
 * <p>In other words, a StreamingKpi converts a somewhat disorganized stream of Point data into a
 * stream of Tracks or TrackPairs that an Event Detection algorithm can process directly.
 *
 * @param <T> The Event Detection algorithm "at the heart" this StreamingKpi (which must implement
 *            either {@literal Consumer<Track>} or {@literal Consumer<TrackPair>})
 */
public class StreamingKpi<T> implements Consumer<Point> {

    /**
     * Sets the "Point sort" Duration used in the StreamingPointSorter. This impacts the amount of
     * memory that is allocated to storing Point data BEFORE that data is released to be processed
     * by the KPI.
     */
    public static final String IN_MEMORY_SORT_BUFFER_SEC = "in.memory.sort.buffer.sec";

    /* Corrects out-of-time-sequence flaws in the incoming Point data. */
    private final StreamingTimeSorter pointSorter;

    /* Enables queries into the total number of tracks processed */
    private final TrackMaker trackMaker;

    /* Enables queries into the total number of trackPairs processed */
    private final TrackPairer trackPairer;

    /*
     * The Event Detection algorithm at the "heart" of this KPI. The coreLogic is kept in a
     * dedicated field (as the generic type T) so that it is possible to "reach in" and extract
     * highly specific information like "aStreamingKpi.coreLogic().numEventsOnTuesday()". Most
     * statistic gathering methods like this don't warrant an API-footprint in this class.
     * nevertheless, it is worth having some way to cleanly support methods like these.
     */
    private final T coreLogic;

    private long curPointCount;

    /**
     * Create a StreamingKpi that can accept a stream of Point data and convert that Point stream
     * into a Stream of TrackPairs that the trackPairAnalyzer can operate on.
     *
     * @param <T>               A class that implements an Event Detection algorithm which operates
     *                          on TrackPairs.
     * @param trackPairAnalyzer An instance of the Event Detection algorithm. This instantiation is
     *                          responsible for ensuring that detected events are emitted to some
     *                          location (i.e. the local file system, a database, or a kafka
     *                          cluster) for permanent storage.
     * @param pairingConfig     Defines what "close" means when creating TrackPairs.
     * @param inMemoryBufferSec How much Point data is kept in memory to smooth out timing errors
     *
     * @return A StreamingKpi that analyzes TrackPairs.
     */
    public static <T extends Consumer<TrackPair>> StreamingKpi<T> trackPairKpi(T trackPairAnalyzer, PairingConfig pairingConfig, int inMemoryBufferSec) {

        TrackPairer trackPairer = new TrackPairer(trackPairAnalyzer, pairingConfig);

//        int inMemoryBufferSec = getInt(IN_MEMORY_SORT_BUFFER_SEC, combinedProps);

        StreamingTimeSorter pointSorter = new StreamingTimeSorter(
            trackPairer,
            Duration.ofSeconds(inMemoryBufferSec)
        );

        return new StreamingKpi<>(trackPairAnalyzer, trackPairer, pointSorter);
    }

    /**
     * Create a StreamingKpi that can accept a stream of Point data and convert that Point stream
     * into a Stream of Tracks that the trackAnalyzer can operate on.
     *
     * @param <T>           A class that implements an Event Detection algorithm which operates on
     *                      Tracks.
     * @param trackAnalyzer An instance of the Event Detection algorithm. This instantiation is
     *                      responsible for ensuring that detected events are emitted to some
     *                      location (i.e. the local file system, a database, or a kafka cluster)
     *                      for permanent storage.
     *
     * @return A StreamingKpi that analyzes Tracks.
     */
    public static <T extends Consumer<Track>> StreamingKpi<T> trackBasedKpi(T trackAnalyzer, Properties properties) {

        TrackMaker trackMaker = new TrackMaker(trackAnalyzer);

        int inMemoryBufferSec = getInt(IN_MEMORY_SORT_BUFFER_SEC, properties);

        StreamingTimeSorter pointSorter = new StreamingTimeSorter(
            trackMaker,
            Duration.ofSeconds(inMemoryBufferSec)
        );

        return new StreamingKpi<>(trackAnalyzer, trackMaker, pointSorter);
    }

    private StreamingKpi(T coreLogic, TrackMaker trackMaker, StreamingTimeSorter pointSorter) {
        this.coreLogic = checkNotNull(coreLogic);
        this.trackMaker = checkNotNull(trackMaker);
        this.pointSorter = checkNotNull(pointSorter);
        this.trackPairer = null;
        this.curPointCount = 0L;
    }

    // FIXME: resolve duplication issues and make this private again (?)
    public StreamingKpi(T coreLogic, TrackPairer trackPairer, StreamingTimeSorter pointSorter) {
        this.coreLogic = checkNotNull(coreLogic);
        this.trackPairer = checkNotNull(trackPairer);
        this.pointSorter = checkNotNull(pointSorter);
        this.trackMaker = trackPairer.innerTrackMaker();
        this.curPointCount = 0L;
    }

    /**
     * @return The Event detection algorithm "at the heart" of this StreamingKpi. Note: this method
     *     returns the exact object that receives and analyzes Track or TrackPairs. Consequently,
     *     this method is often used to query the event detection algorithm for status information
     *     like "numHighSeverityEvent" or "numEventNearAirport".
     */
    public T coreLogic() {
        return coreLogic;
    }

    @Override
    public void accept(Point t) {
        curPointCount++;
        pointSorter.accept(t);
        heartbeat();
    }

    private void heartbeat() {
        if (curPointCount % 100_000 == 0) {
            System.out.println("  Processed Point: "
                + NumberFormat.getNumberInstance(Locale.US).format(curPointCount));
        }
    }

    public long numPointsProcessed() {
        return this.curPointCount;
    }

    public long numTracksProcessed() {
        return trackMaker().numTracksPublished();
    }

    public long numTrackPairsProcessed() {
        return (isNull(trackPairer))
            ? 0
            : trackPairer.numTrackPairsIdentified();
    }

    public StreamingTimeSorter pointSorter() {
        return this.pointSorter;
    }

    public TrackMaker trackMaker() {
        return trackMaker;
    }

    public PointPairFinder pointPairFinder() {
        return (isNull(trackPairer))
            ? null
            : trackPairer.innerPairFinder();
    }

    /**
     * Flush all Point data that is stored in this StreamingKpi's inner Point sorter.
     */
    public void flush() {
        pointSorter.flush();
        trackMaker.flushAllTracks();
    }
}
