

package org.mitre.openaria.threading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static org.mitre.caasd.commons.Spherical.distanceInNM;
import static org.mitre.caasd.commons.Time.confirmStrictTimeOrdering;
import static org.mitre.caasd.commons.Time.durationBtw;
import static org.mitre.caasd.commons.Time.theDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.mitre.openaria.core.KeyExtractor;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.util.ParallelismDetector;

/**
 * A TrackMaker combines Points from a time-sorted stream of Points to make CommonTracks. Once a
 * CommonTrack is closed it is "pushed to" the {@literal Consumer<Track>} provided at construction.
 *
 * <p>A TrackMaker combines multiple Points into a Track if: (A) the Points have the same trackId
 * and facility, (B) all points in a Track are close together in physical space, and (C) the "next"
 * Point in a Track always occurs within a fixed window of time of the "prior" point in that track.
 *
 * <p>Note, a, ApproximateTimeSorter and/or a StrictTimeSortEnforcer can help ensure Points
 * supplied to this TrackMaker meet the "all Points are time-sorted" requirement.
 *
 * <p>Warning: TrackMaker is not thread-safe. It is a mistake to call "accept(Point)" in one
 * thread and "flushAllTracks()" in another thread.
 */
public class TrackMaker implements Consumer<Point> {

    /** The default maximum amount of time allowed between two points of the same track. */
    private static final Duration DEFAULT_MAX_TIME_DELTA = Duration.ofSeconds(45);

    /**
     * The default maximum track age before tracks are forcibly closed and pushed downstream. Forced
     * track closing prevents memory-leaks caused by tracks that NEVER end (a problem caused by
     * certain types of radar noise AND real-world aircraft like blimps and surveillance aircraft).
     */
    private static final Duration DEFAULT_TRACK_CLOSURE_AGE = Duration.ofHours(2);

    /**
     * Tracks under construction are automatically closed when they grow too long. This prevents a
     * memory-leak in which a TrackUnderConstruction that never closes eventually crashes the entire
     * system.
     */
    private final Duration forcedTrackClosureAge;

    /**
     * The maximum amount of time between two points of the same Track.
     */
    private final Duration maxTimeBetweenTrackPoints;

    /** This time only increases. Thus, the currentTime is also the "time high water mark" */
    private Instant currentTime;

    /** Makes "join keys" that group individual Points into groups of Points that become Tracks. */
    private final KeyExtractor<Point> keyExtractor;

    /**
     * This HashMap's iteration order reflects LRU-ACCESS order. We can quickly iterate its entries
     * to find stale tracks that need to be removed.
     */
    private final LinkedHashMap<String, TrackUnderConstruction> tracksUnderConstruction;

    private final Consumer<Track> outputMechanism;

    private int currentSize = 0;

    /*
     * The highest number of Points ever held in this TrackMaker's family of Tracks Under
     * Construction. This value gives insight into how much memory is used by the TrackMaker.
     */
    private int sizeHighWaterMark = 0;

    private int numTracksHighWaterMark = 0;

    private long numPointsPublished = 0;

    private long numTracksPublished = 0;

    /** Ensures "accept(Point)" and "flushAllTracks()" are never called in parallel. */
    private ParallelismDetector parallelismDetector = new ParallelismDetector();

    /**
     * A TrackMaker generates CommonTracks by combining Points in that have the same Facility,
     * trackId, and sensor.
     *
     * @param maxTimeBetweenPoints The maximum time between any two points that can be from the same
     *                             CommonTrack.
     * @param maxTrackAge          Tracks longer than this are automatically closed and published to
     *                             prevent memory leaks and reduce latency
     * @param outputMechanism      Where complete tracks are sent to after being removed from this
     *                             Object
     */
    public TrackMaker(Duration maxTimeBetweenPoints, Duration maxTrackAge, Consumer<Track> outputMechanism) {
        this.maxTimeBetweenTrackPoints = checkNotNull(maxTimeBetweenPoints);
        this.forcedTrackClosureAge = checkNotNull(maxTrackAge);
        checkArgument(theDuration(maxTrackAge).isGreaterThan(maxTimeBetweenPoints));
        this.currentTime = null;
        this.keyExtractor = Point.keyExtractor();
        this.tracksUnderConstruction = new LinkedHashMap<>(16, 0.75f, true);
        this.outputMechanism = outputMechanism;
    }

    /**
     * A TrackMaker generates CommonTracks by combining Points in that have the same Facility,
     * trackId, and sensor.
     *
     * @param maxTimeBetweenPoints The maximum time between any two points that can be from the same
     *                             CommonTrack.
     * @param outputMechanism      Where complete tracks are sent to after being removed from this
     *                             Object
     */
    public TrackMaker(Duration maxTimeBetweenPoints, Consumer<Track> outputMechanism) {
        this(maxTimeBetweenPoints, DEFAULT_TRACK_CLOSURE_AGE, outputMechanism);
    }

    /**
     * A TrackMaker generates CommonTracks by combining Points in that have the same Facility,
     * trackId, and sensor. This constructor uses a default {@link #DEFAULT_MAX_TIME_DELTA}.
     *
     * @param outputMechanism Where complete tracks are sent to after being removed from this
     *                        Object
     */
    public TrackMaker(Consumer<Track> outputMechanism) {
        this(DEFAULT_MAX_TIME_DELTA, DEFAULT_TRACK_CLOSURE_AGE, outputMechanism);
    }

    @Override
    public void accept(Point newPoint) {

        //ensure this method is never called by multiple threads at the same time.
        parallelismDetector.run(
            () -> doAccept(newPoint)
        );
    }

    private void doAccept(Point newPoint) {
        updateTimeAndConfirmOrdering(newPoint.time());

        addPointToTrack(newPoint);

        sizeHighWaterMark = max(sizeHighWaterMark, currentSize);
        numTracksHighWaterMark = max(numTracksHighWaterMark, tracksUnderConstruction.size());

        closeStaleTracks();
    }

    private void updateTimeAndConfirmOrdering(Instant candidateTime) {

        if (currentTime == null) {
            currentTime = candidateTime;
            return;
        }

        confirmStrictTimeOrdering(currentTime, candidateTime);
        currentTime = candidateTime;
    }

    private void addPointToTrack(Point newPoint) {

        String key = keyExtractor.joinKeyFor(newPoint);

        if (tracksUnderConstruction.containsKey(key)) {
            considerAddingPointToAnExistingTrack(key, newPoint);
        } else {
            startNewTrack(key, newPoint);
        }
    }

    private void considerAddingPointToAnExistingTrack(String key, Point newPoint) {

        TrackUnderConstruction existingTrack = tracksUnderConstruction.get(key);

        if (canAddPointToExistingTrack(existingTrack, newPoint)) {
            existingTrack.addPoint(newPoint);
            currentSize++;
        } else {
            /*
             * The existing TrackUnderConstruction cannot "accept" the newPoint. Therefore, we close
             * that TrackUnderConstruction and start a new one.
             */
            closeAndPublish(newArrayList(key));
            startNewTrack(key, newPoint);
        }
    }

    /**
     * Return True if the candidatePoint can be added to the TrackUnderConstruction. Currently, this
     * requires the candidate point to be "within range" both physically (in distance) and
     * temporally.
     *
     * @param tuc            A TrackUnderConstruction with a "lastPoint"
     * @param candidatePoint A Point that is being considered for addition to the existing track.
     *
     * @return True, if the required conditions are met.
     */
    private boolean canAddPointToExistingTrack(TrackUnderConstruction tuc, Point candidatePoint) {

        return isCloseInDistance(tuc, candidatePoint)
            && isCloseInTime(tuc, candidatePoint)
            && doesNotExceedMaxTime(tuc, candidatePoint);
    }

    private boolean isCloseInDistance(TrackUnderConstruction tuc, Point candidatePoint) {
        /*
         * We know the new point comes at the very end of the track because all input Points MUST
         * arrive in time order.
         */
        Point lastPoint = tuc.lastPoint();
        double distInNM = distanceInNM(lastPoint.latLong(), candidatePoint.latLong());

        final double MAX_DISTANCE_IN_NM = 5.0;
        return distInNM < MAX_DISTANCE_IN_NM;
    }

    private boolean isCloseInTime(TrackUnderConstruction tuc, Point candidatePoint) {

        Duration timeDelta = tuc.timeSince(candidatePoint.time());
        return timeDelta.minus(maxTimeBetweenTrackPoints).isNegative();
    }

    private boolean doesNotExceedMaxTime(TrackUnderConstruction tuc, Point candidatePoint) {

        Duration trackDurationWithNewPoint = durationBtw(
            candidatePoint.time(),
            tuc.timeOfEarliestPoint(),
            tuc.timeOfLatestPoint()
        );
        return trackDurationWithNewPoint.toMillis() < forcedTrackClosureAge.toMillis();
    }

    private void startNewTrack(String key, Point seedPoint) {
        currentSize++;
        this.tracksUnderConstruction.put(key, new TrackUnderConstruction(seedPoint));
    }

    /** Find and close tracks that haven't received data recently. */
    private void closeStaleTracks() {
        closeAndPublish(findStaleTracks());
    }

    private HashSet<String> findStaleTracks() {

        HashSet<String> staleTracks = new HashSet<>();

        Iterator<Map.Entry<String, TrackUnderConstruction>> iter = tracksUnderConstruction.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TrackUnderConstruction> entry = iter.next();

            TrackUnderConstruction candidateForClosure = entry.getValue();
            String keyOfCandidate = entry.getKey();

            if (trackDataIsStale(candidateForClosure)) {
                staleTracks.add(keyOfCandidate);
            } else {
                /*
                 * we know the Map.Entry iteration occurs in LRU order. So once we start seeing
                 * non-stale tracks EVERY subsequent track will be non-stale so we don't have to
                 * search anymore
                 */
                break;
            }
        }

        return staleTracks;
    }

    private boolean trackDataIsStale(TrackUnderConstruction workingTrack) {

        Duration timeDelta = workingTrack.timeSince(currentTime);

        return maxTimeBetweenTrackPoints.minus(timeDelta).isNegative();
    }

    private void closeAndPublish(Collection<String> closeTheseTracks) {
        closeTheseTracks.forEach(key -> removeAndPublish(key));
    }

    private void removeAndPublish(String key) {
        TrackUnderConstruction closeMe = tracksUnderConstruction.remove(key);

        /*
         * In rare cases, this "closeMe" TrackUnderConstruction came back as null. The root cause
         * was traced back to unwanted parallelism when between one Thread calling "accept(Point)"
         * and another thread calling "flushAllTracks()" during a Kafka ConsumerGroup rebalance
         * event.
         *
         * The solution to the vulnerability is to (1) add a potentially unnecessary null check
         * right here to harden this method and (2) prevent the parallel calls to "accept(Point)"
         * and "flushAllTracks()" by redesigning how rebalance events are handled. Now, we schedule
         * a one-time flushAllTracks() operation to fire after a regular data processing task
         * completes (see end of SwimLane::processQueuedData).  Previously, we IMMEDIATELY called
         * flushAllTracks() and that sometimes (incorrectly) occurred in parallel with the
         */
        if (isNull(closeMe)) {
            return;
        }

        currentSize -= closeMe.size();
        numPointsPublished += closeMe.size();
        numTracksPublished++;

        Track trackToPublish = new SimpleTrack(closeMe.points());
        outputMechanism.accept(trackToPublish);
    }

    /**
     * Forcibly push all "tracks under construction" to the outputMechanism. This method is
     * generally used when a data source reaches its ends and we don't want important data (i.e.
     * unclosed tracks) stranded in the innards of this TrackMaker.
     *
     * <p>Calling this method in parallel with "accept(Point)" or a 2nd call to "flushAllTracks()"
     * will fail.
     */
    public void flushAllTracks() {

        parallelismDetector.run(() -> {
            ArrayList<String> allKnownKeys = newArrayList(tracksUnderConstruction.keySet());
            closeAndPublish(allKnownKeys);
        });
    }

    public int currentPointCount() {
        return currentSize;
    }

    public int numTracksUnderConstruction() {
        return tracksUnderConstruction.size();
    }

    public int numTracksUnderConstructionHighWaterMark() {
        return numTracksHighWaterMark;
    }

    public long numPointsPublished() {
        return numPointsPublished;
    }

    public long numTracksPublished() {
        return numTracksPublished;
    }

    /** Tracks are closed as soon as they get this old. */
    public Duration forceTrackClosureAge() {
        return this.forcedTrackClosureAge;
    }

    /**
     * @return The highest number of Points ever held in this TrackMaker's family of Tracks Under
     *     Construction. This value gives insight into how much memory is used by the TrackMaker.
     */
    public int sizeHighWaterMark() {
        return sizeHighWaterMark;
    }
}
