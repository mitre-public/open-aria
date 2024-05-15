
package org.mitre.openaria.trackpairing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.mitre.caasd.commons.Functions.ALWAYS_TRUE;
import static org.mitre.openaria.threading.TempUtils.keyExtractor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.mitre.caasd.commons.Pair;
import org.mitre.openaria.core.KeyExtractor;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.pointpairing.PairingConfig;
import org.mitre.openaria.pointpairing.PointPairFinder;
import org.mitre.openaria.threading.TrackMaker;

/**
 * A TrackPairer ingests a stream of Point data and simultaneously (1) creates Tracks and (2) finds
 * Pairs of Tracks that are close in both time and space.
 * <p>
 * Tracks are piped to the downstream {@literal Consumer<Track>} while TrackPairs are piped to a
 * downstream {@literal Consumer<TrackPair>}.
 * <p>
 * These two tasks are inter-twined here because TrackPairing is a computationally expensive
 * operation. Consequently, performing TrackPairing WHILE performing TrackMaking is basically
 * required to efficiently process streaming Point data.
 * <p>
 * A TrackPairer uses a TrackMaker and a PointPairFinder to perform its work.
 */
public class TrackPairer implements Consumer<Point> {

    private static final Duration MAX_DURATION_BETWEEN_PTS_OF_SAME_TRACK = Duration.ofSeconds(45);

    /** Makes "join keys" that group individual Points into groups of Points that become Tracks. */
    private final KeyExtractor<Point> keyExtractor;

    private final TrackMaker trackMaker;
    private final PointPairFinder pairFinder;
    private final Predicate<Pair<Point, Point>> trackPairingRequirement;
    private final HashMap<String, OpenTrackPair> openTrackPairs_noTracks;
    private final List<OpenTrackPair> openTrackPairs_oneTrack;
    private final Consumer<Track> downstreamTrackConsumer;
    private final Consumer<TrackPair> downstreamPairConsumer;
    private long numTrackPairsIdentified;

    /**
     * Create a TrackPairer that pipes all TrackPairs to this downstream consumer
     *
     * @param downstreamPairConsumer This consumer receives all TrackPairs that are created.
     * @param props                  Defines what "close" means when finding Points that are close
     *                               in time and space. These "close Point pairs" lead to
     *                               TrackPairs
     */
    public TrackPairer(Consumer<TrackPair> downstreamPairConsumer, PairingConfig props) {
        this(new ConsumerPair(null, downstreamPairConsumer), props);
    }

    /**
     * Create a TrackPairer that pipes all Tracks and TrackPairs to their respected downstream
     * consumers
     *
     * @param downstreamTrackConsumer This consumer receives all Track that are created.
     * @param downstreamPairConsumer  This consumer receives all TrackPairs that are created.
     * @param props                   Defines what "close" means when finding Points that are close
     *                                in time and space. These "close Point pairs" lead to
     *                                TrackPairs
     */
    public TrackPairer(Consumer<Track> downstreamTrackConsumer, Consumer<TrackPair> downstreamPairConsumer, PairingConfig props) {
        this(new ConsumerPair(downstreamTrackConsumer, downstreamPairConsumer), props);
    }

    /**
     * Create a TrackPairer that pipes all Tracks and TrackPairs to their respected downstream
     * consumers
     *
     * @param downstream These downstream consumers receive Tracks and TrackPairs that are made by
     *                   this TrackPairer.
     * @param props      Defines what "close" means when finding Points that are close in time and
     *                   space. These "close Point pairs" lead to TrackPairs
     */
    public TrackPairer(ConsumerPair downstream, PairingConfig props) {
        this(downstream, props, ALWAYS_TRUE);
    }

    /**
     * Create a TrackPairer that pipes Tracks and TrackPairs to their respected downstream
     * consumers. This TrackPairer requires TrackPairs to meet an additional logic gate. Ordinarily
     * a TrackPairer emits all TrackPairs for which the two Tracks were close in time and space.
     * This constructor allows filtering like "only emit TrackPairs for which the two Tracks were
     * close in time and space AND at the time of Pairing both Tracks had an altitude higher than
     * 10,000 ft". This filter can be helpful to prevent Pairing aircraft when both aircraft are
     * moving extremely slowly (for example when taxiing near the gate environment at an airport).
     *
     * @param downstream              These downstream consumers receive Tracks and TrackPairs that
     *                                are made by this TrackPairer.
     * @param pairingProps            Defines what "close" means when finding Points that are close
     *                                in time and space. These "close Point pairs" lead to
     *                                TrackPairs
     * @param trackPairingRequirement This Predicate must return true in order to prompt two Tracks
     *                                to form a TrackPair. For example, if you only wanted
     *                                TrackPairs in which at least one aircraft was airborne you
     *                                could require that here
     */
    public TrackPairer(ConsumerPair downstream, PairingConfig pairingProps, Predicate<Pair<Point, Point>> trackPairingRequirement) {
        this(downstream, pairingProps, trackPairingRequirement, MAX_DURATION_BETWEEN_PTS_OF_SAME_TRACK);
    }

    /**
     * Create a TrackPairer that pipes Tracks and TrackPairs to their respected downstream
     * consumers. This TrackPairer requires TrackPairs to meet an additional logic gate. Ordinarily
     * a TrackPairer emits all TrackPairs for which the two Tracks were close in time and space.
     * This constructor allows filtering like "only emit TrackPairs for which the two Tracks were
     * close in time and space AND at the time of Pairing both Tracks had an altitude higher than
     * 10,000 ft". This filter can be helpful to prevent Pairing aircraft when both aircraft are
     * moving extremely slowly (for example when taxiing near the gate environment at an airport).
     * The maxTimeBetweenPointsOfSameTrack causes tracks to be split at data gaps that are longer
     * than the specified duration.
     *
     * @param downstream                      These downstream consumers receive Tracks and
     *                                        TrackPairs that are made by this TrackPairer.
     * @param pairingProps                    Defines what "close" means when finding Points that
     *                                        are close in time and space. These "close Point pairs"
     *                                        lead to TrackPairs.
     * @param trackPairingRequirement         This Predicate must return true in order to prompt two
     *                                        Tracks to form a TrackPair.
     * @param maxTimeBetweenPointsOfSameTrack The largest allowable time gap between consecutive
     *                                        points of the same track.
     */
    public TrackPairer(ConsumerPair downstream,
        PairingConfig pairingProps,
        Predicate<Pair<Point, Point>> trackPairingRequirement,
        Duration maxTimeBetweenPointsOfSameTrack) {

        checkNotNull(downstream, "Must provide downstream consumers for the TrackPairer");
        checkNotNull(maxTimeBetweenPointsOfSameTrack, "maxTimeBetweenPointsOfSameTrack cannot be null");
        this.downstreamPairConsumer = downstream.pairConsumer();
        this.downstreamTrackConsumer = downstream.trackConsumer();
        this.trackPairingRequirement = checkNotNull(trackPairingRequirement);

        this.keyExtractor = keyExtractor();

        //when the internal TrackMaker produces a track call "incorporateNewTrack"
        this.trackMaker = new TrackMaker(maxTimeBetweenPointsOfSameTrack, this::incorporateNewTrack);

        //configure a PointPairFinder that notifies this TrackPairMaker whenever "close Pair of Points" is found
        this.pairFinder = new PointPairFinder(pairingProps, this::incorporateNewPointPair);
        this.openTrackPairs_noTracks = newHashMap();
        this.openTrackPairs_oneTrack = newArrayList();
        this.numTrackPairsIdentified = 0L;
    }

    private void incorporateNewTrack(Track newTrack) {

        publishTrackToDedicatedConsumer(newTrack);

        ArrayList<OpenTrackPair> closeThese = updateOpenPairsAndFindPublishablePairs(newTrack);

        publishPairs(closeThese);
    }

    private void publishTrackToDedicatedConsumer(Track newTrack) {
        if (nonNull(downstreamTrackConsumer)) {
            downstreamTrackConsumer.accept(newTrack);
        }
    }

    private ArrayList<OpenTrackPair> updateOpenPairsAndFindPublishablePairs(Track newTrack) {

        ArrayList<OpenTrackPair> promoteThese = newArrayList(); //saves OTPs that went from 0 to 1 tracks
        ArrayList<OpenTrackPair> closeThese = newArrayList(); //saves OTPs that went from 1 to 2 tracks

        /*
         * Determine if any "OpenTrackPairs with no tracks" need to be promoted to "an OpenTrackPair
         * with one track". This promotion process "leaves room" for another OpenTrackPair that will
         * reuse the same "join key". This is necessary because sometime a long-lived track will
         * need to be pair to multiples tracks that have the same trackId. If we don't promote the
         * "OpenTrackPairs with one track" to a separate list then the duplicates joinkeys will
         * cause a missed pair because one of the tracks will get overwritten.
         */
        for (OpenTrackPair otp : openTrackPairs_noTracks.values()) {
            boolean trackWasAdded = otp.offer(newTrack);
            if (trackWasAdded) {
                promoteThese.add(otp);
            }
        }

        /*
         * Find any OpenTrackPairs (that already have ONE track) that will become "full TrackPairs"
         * upon adding the 2nd track. These OpenTrackPairs will be closed and published.
         */
        for (OpenTrackPair otp : openTrackPairs_oneTrack) {
            boolean trackWasAdded = otp.offer(newTrack);
            if (trackWasAdded) {
                closeThese.add(otp);
            }
        }

        //perform promotions
        for (OpenTrackPair otp : promoteThese) {
            String key = otp.asKey();
            openTrackPairs_noTracks.remove(key);
            openTrackPairs_oneTrack.add(otp);
        }

        return closeThese;
    }

    private void publishPairs(ArrayList<OpenTrackPair> closeThese) {
        for (OpenTrackPair closeMe : closeThese) {
            numTrackPairsIdentified++;
            openTrackPairs_oneTrack.remove(closeMe);
            downstreamPairConsumer.accept(closeMe.asFullTrackPair());
        }
    }

    //Ingest information saying "these two aircraft are so close they should be paired together"
    private void incorporateNewPointPair(Pair<Point, Point> newPair) {

        if (pointsComeFromSameTracks(newPair)) {
            return;
        }

        if (!trackPairingRequirement.test(newPair)) {
            return;
        }

        OpenTrackPair otp = new OpenTrackPair(
            keyExtractor.joinKeyFor(newPair.first()),
            keyExtractor.joinKeyFor(newPair.second())
        );

        openTrackPairs_noTracks.putIfAbsent(otp.asKey(), otp);
    }

    private boolean pointsComeFromSameTracks(Pair<Point, Point> newPair) {
        return newPair.first().trackId().equals(newPair.second().trackId());
    }

    public TrackMaker innerTrackMaker() {
        return this.trackMaker;
    }

    public PointPairFinder innerPairFinder() {
        return this.pairFinder;
    }

    public long numTrackPairsIdentified() {
        return this.numTrackPairsIdentified;
    }

    @Override
    public void accept(Point t) {
        if (t.trackIdIsMissing()) {
            return;
        }
        this.pairFinder.accept(t);
        this.trackMaker.accept(t);
    }

    /**
     * An OpenTrackPair is a place holder for a Pair<Track, Track> that we want are building on the
     * fly.
     * <p>
     * An OpenTrackPair is necessary because we don't yet have the Tracks that will go in this pair.
     * An OpenTrackPair is initially built when a close pair of Point is found. An OpenTrackPair is
     * "closed" once both tracks have been found.
     */
    private class OpenTrackPair {

        private final String key1;
        private final String key2;

        private Track track1;
        private Track track2;

        OpenTrackPair(String inputKey1, String inputKey2) {

            if (inputKey1.compareTo(inputKey2) > 0) {
                this.key1 = inputKey1;
                this.key2 = inputKey2;
            } else {
                this.key1 = inputKey2;
                this.key2 = inputKey1;
            }

            track1 = null;
            track2 = null;
        }

        /* return True if the offered Track was "accepted" as part of the pair. */
        public boolean offer(Track track) {

            Point firstPoint = ((NavigableSet<Point<?>>) track.points()).first();

            String trackKey = keyExtractor.joinKeyFor(firstPoint);

            /*
             * DO NOT allow "track1" or "track2" to be set twice. This can lead to lost events if
             * one of the Tracks in the pair is a long-lived track and the other track in the pair
             * is a short lived track. In this case the TrackID of the short-lived track(s) can be
             * used multiple times while the long-lived track is still opened.
             *
             * Opting to ALWAYS use the 1st track that has the correct TrackJoinKey can (in very
             * rare cases) still permit event to be missed. However, the subsequent tracks (that
             * also happen to have the same TrackJoinKey) are not GUARANTEED to have come close to
             * the long-lived track.
             */
            if (track1 == null && trackKey.equals(key1.toString())) {
                this.track1 = track;
                return true;
            }

            if (track2 == null && trackKey.equals(key2.toString())) {
                this.track2 = track;
                return true;
            }

            return false;
        }

        public TrackPair asFullTrackPair() {
            if (isNull(track1) || isNull(track2)) {
                throw new IllegalStateException("Cannot convert to Pair before both tracks are known");
            }
            return new TrackPair(track1, track2);
        }

        /** @return A key that can be used to put/get this objects in a Map. */
        public String asKey() {
            return key1.toString() + "_" + key2.toString();
        }
    }
}
