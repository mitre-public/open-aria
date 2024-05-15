package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Streams.stream;
import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.airborne.AirborneEvent.newBuilder;
import static org.mitre.openaria.airborne.Snapshot.extractSnapshot;
import static org.mitre.openaria.airborne.Snapshot.extractSnapshotWithCpa;
import static org.mitre.openaria.core.SeparationTimeSeries.lateralComparator;
import static org.mitre.openaria.core.SeparationTimeSeries.verticalComparator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Triple;
import org.mitre.openaria.core.ClosestPointOfApproach;
import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.TrackPair;

public class AirborneAria {

    private final AirborneAlgorithmDef algorithmDef;

    /** Events with scores above this threshold are ignored. */
    private final double maxReportableScore;

    /** When true events that are "outside" a particular facility's airspace will be muted. */
    private final boolean filterByAirspace;

    /** When True only events that contain at least one data tag will be included. */
    private final boolean requireDataTag;

    /** Removed "data imperfections" from the raw input data. */
    private final DataCleaner<TrackPair> dataCleaner;

    /**
     * This logic analyzes an AirborneEvent to determine if is important enough to emit as output
     * when the findAirborneEvents method is called.
     */
    private final Predicate<AirborneEvent> shouldPublish;

    private AirborneAria(AirborneAlgorithmDef algorithmDef) {
        requireNonNull(algorithmDef);
        this.algorithmDef = algorithmDef;
        this.maxReportableScore = algorithmDef.maxReportableScore();
        this.filterByAirspace = algorithmDef.filterByAirspace();
        this.requireDataTag = algorithmDef.requireAtLeastOneDataTag();
        this.dataCleaner = algorithmDef.pairCleaner();
        this.shouldPublish = new TempShouldPublishPredicate(maxReportableScore, requireDataTag);
    }

    /** Create a new AirborneAria algorithm that uses the default properties. */
    public static AirborneAria airborneAria() {
        return new AirborneAria(new AirborneAlgorithmDef());
    }

    /** Create a new AirborneAria algorithm that uses custom definition. */
    public static AirborneAria airborneAria(AirborneAlgorithmDef definition) {
        return new AirborneAria(definition);
    }

    public ArrayList<AirborneEvent> findAirborneEvents(TrackPair trackPair) {

        Optional<TrackPair> smoothingResult = dataCleaner.clean(trackPair);

        if (!smoothingResult.isPresent()) {
            return newArrayList();  //smoothing rejected the input
        }

        AirborneEvent event = performAnalysisAndExtractEvent(trackPair, smoothingResult.get());

        return shouldPublish.test(event)
            ? newArrayList(event)
            : newArrayList();
    }

    /**
     * This Predicate is a temporary stand-in for a more nuanced Event Filtering approach.  The "old
     * code" had this logic baked into the workflow.  This is step one towards refactoring the logic
     * to have "what is an event" separated from "which events get published"
     */
    static class TempShouldPublishPredicate implements Predicate<AirborneEvent> {

        private final double maxPublishableScore;

        private final boolean requireDataTag;

        private final boolean passesFormationFilter = true;

        private final boolean meetsAirspaceReq = true;

        TempShouldPublishPredicate(double maxReportableScore, boolean requireDataTag) {
            checkArgument(0 < maxReportableScore);
            this.maxPublishableScore = maxReportableScore;
            this.requireDataTag = requireDataTag;
        }

        @Override
        public boolean test(AirborneEvent event) {

            boolean reportableScore = event.score() <= maxPublishableScore;

            boolean meetsDataTagReq = requireDataTag ? event.containsAircraftWithDataTag() : true;

            return reportableScore && passesFormationFilter && meetsDataTagReq && meetsAirspaceReq;
        }
    }


    private AirborneEvent performAnalysisAndExtractEvent(TrackPair rawPair, TrackPair smoothedPair) {

        TrackPairAnalysis tpa = new TrackPairAnalysis(smoothedPair);

        SerializableAnalysis dynamics = algorithmDef.publishDynamics()
            ? tpa.analysis.truncate(algorithmDef.dynamicsInclusionRadius()).serializedForm() :
            null;

        AirborneEvent event = newBuilder()
            .rawTracks(rawPair)
            .smoothedTracks(smoothedPair)
            .riskiestMoment(tpa.riskiestMoment)
            .snapshots(extractKeyMoments(smoothedPair, tpa))
            .dynamics(dynamics)
            .includeTrackData(algorithmDef.publishTrackData())
            .build();

        return event;
    }

    /**
     * Collect Snapshots from important moments in time for these two aircraft.
     *
     * @return Snapshot[] {atEventTime, atEstimatedCpaTime, atClosestLateral,
     *     atClosestLateralWith1kVert, atClosestVerticalWith3Nm, atClosestVerticalWith5Nm}
     */
    private Snapshot[] extractKeyMoments(TrackPair smoothedPair, TrackPairAnalysis tpa) {

        Instant eventTime = tpa.riskiestMoment.time();
        ClosestPointOfApproach cpa = smoothedPair.interpolatedPointsAt(eventTime).closestPointOfApproach();

        Instant timeOfCpa = eventTime.plus(cpa.timeUntilCpa());
        Instant timeOfClosestLateral = timeOfClosestLateral(smoothedPair);
        Instant timeOfClosestLateralWith1kVert = timeOfClosestLateralWith1kVert(smoothedPair);
        Instant timeOfClosestVerticalWith3Nm = timeOfClosestVerticalWith3Nm(smoothedPair);
        Instant timeOfClosestVerticalWith5Nm = timeOfClosestVerticalWith5Nm(smoothedPair);

        Snapshot atEventTime = extractSnapshotWithCpa(smoothedPair, tpa.riskiestMoment);
        Snapshot atEstimatedCpaTime = snapshotAt(tpa, timeOfCpa);
        Snapshot atClosestLateral = snapshotAt(tpa, timeOfClosestLateral);
        Snapshot atClosestLateralWith1kVert = snapshotAt(tpa, timeOfClosestLateralWith1kVert);
        Snapshot atClosestVerticalWith3Nm = snapshotAt(tpa, timeOfClosestVerticalWith3Nm);
        Snapshot atClosestVerticalWith5Nm = snapshotAt(tpa, timeOfClosestVerticalWith5Nm);

        Snapshot[] keyMoments = new Snapshot[]{
            atEventTime,
            atEstimatedCpaTime,
            atClosestLateral,
            atClosestLateralWith1kVert,
            atClosestVerticalWith3Nm,
            atClosestVerticalWith5Nm
        };
        return keyMoments;
    }

    private Snapshot snapshotAt(TrackPairAnalysis tpa, Instant time) {

        //Extracting a snapshot requires the score -- so see if the score is defined at this time
        ScoredInstant si = tpa.closestScoredMoment(time);

        return (si == null)
            ? null
            : extractSnapshot(tpa.sourcePair, si.time(), si.score());
    }

    private Instant timeOfClosestLateral(TrackPair smoothedPair) {

        return stream(smoothedPair.separationInfo().timeLatVertIterator())
            .min(lateralComparator()) //sort by lateral distance
            .get()
            .first();
    }

    private Instant timeOfClosestLateralWith1kVert(TrackPair smoothedPair) {

        Optional<Triple<Instant, Distance, Distance>> opt = stream(smoothedPair.separationInfo().timeLatVertIterator())
            .filter(triple -> triple.third().isLessThanOrEqualTo(Distance.ofFeet(1000)))
            .min(lateralComparator()); //sort by lateral distance

        return opt.isPresent()
            ? opt.get().first()
            : null;
    }

    private Instant timeOfClosestVerticalWith3Nm(TrackPair smoothedPair) {

        Optional<Triple<Instant, Distance, Distance>> opt = stream(smoothedPair.separationInfo().timeLatVertIterator())
            .filter(triple -> triple.second().isLessThanOrEqualTo(Distance.ofNauticalMiles(3.0)))
            .min(verticalComparator()); //sort by vertical distance

        return opt.isPresent()
            ? opt.get().first()
            : null;
    }

    private Instant timeOfClosestVerticalWith5Nm(TrackPair smoothedPair) {
        Optional<Triple<Instant, Distance, Distance>> opt = stream(smoothedPair.separationInfo().timeLatVertIterator())
            .filter(triple -> triple.second().isLessThanOrEqualTo(Distance.ofNauticalMiles(5.0)))
            .min(verticalComparator()); //sort by vertical distance

        return opt.isPresent()
            ? opt.get().first()
            : null;
    }

    public DataCleaner<TrackPair> dataCleaner() {
        return this.dataCleaner;
    }

    /**
     * @return a Comparator that sorts ScoredInstants by how close their time value is to the
     *     "seekTime".
     */
    private static Comparator<ScoredInstant> closestToTimeComparator(Instant seekTime) {
        Comparator<ScoredInstant> comp = (ScoredInstant one, ScoredInstant two) -> {
            long oneDelta = abs(one.time().toEpochMilli() - seekTime.toEpochMilli());
            long twoDelta = abs(two.time().toEpochMilli() - seekTime.toEpochMilli());

            if (oneDelta == twoDelta) {
                return 0;
            } else if (oneDelta > twoDelta) {
                return 1;
            } else {
                return -1;
            }
        };
        return comp;
    }

//    /**
//     * Applies a Formation Flight Filter -- Then overrides it when events occur near a tower. The
//     * reason for this is that when aircraft compress during landing they ARE NOT a standard
//     * military or stunt plane formation, thus these parallel approaches should not be excluded
//     * using this filter.
//     */
//    static class TowerAwareFormationFilter {
//
//        DataCleaner<TrackPair> formationFilter;
//
//        TowerAwareFormationFilter(List<IsFormationFlight.FormationFilterDefinition> defs) {
//            this.formationFilter = formationFlightFilter(defs);
//        }
//
//        public boolean shouldPublish(AirborneEvent event) {
//
//            Optional<TrackPair> postFilter = formationFilter.clean(event.bestAvailableData());
//
//            //if the TrackPair was not in formation, so it should always be published
//            if (postFilter.isPresent()) {
//                return true;
//            }
//
//            //the TrackPair was in formation...
//            return event.isNearTower(); //so we will publish it IF the riskiestMoment occured near a tower
//        }
//    }

    /*
     * Given a TrackPair: compute the ARIA score time-series, the moving sums of this data, and
     * make key points in the time-series queryable
     */
    static class TrackPairAnalysis {

        final TrackPair sourcePair;
        final AirborneAnalysis analysis;
        final ArrayList<ScoredInstant> movingSums;
        final ScoredInstant riskiestMoment;

        TrackPairAnalysis(TrackPair pair) {
            this.sourcePair = pair;
            this.analysis = new AirborneAnalysis(pair);
            this.movingSums = analysis.computeMovingSums();
            this.riskiestMoment = movingSums.stream()
                .sorted()
                .findFirst()
                .get();
        }

        ScoredInstant closestScoredMoment(Instant queryTime) {

            //The queryTime is missing OR the queryTime is not in the "trackOverlap"
            if (queryTime == null || !sourcePair.timeOverlap().get().contains(queryTime)) {
                return null;
            }

            //find the closest ScoredInstant by using a comparator that sorts by time
            return movingSums.stream()
                .sorted(closestToTimeComparator(queryTime))
                .findFirst()
                .get();
        }
    }

    /**
     * Extract the score of a TrackPair at exactly one moment in time. Note: The current
     * implementation does not reduce the runtime because it merely extracts the correct value from
     * a complete end-to-end analysis.
     *
     * @param trackPair
     * @param time
     *
     * @return The score at this moment in Time.
     */
    public double cherryPickScore(TrackPair trackPair, Instant time) {
        checkNotNull(trackPair, "Input TrackPair cannot be null");
        checkNotNull(time, "Input Instant cannot be null");
        checkArgument(trackPair.overlapInTime(), "Tracks in TrackPair must overlap in time");
        checkArgument(trackPair.timeOverlap().get().contains(time), "Specifed time value is not within the tracks time overlap");

        //recompute the ENTIRE ANALYSIS to extrac the output for this one moment in time
        TrackPairAnalysis tpa = new TrackPairAnalysis(trackPair);

        for (ScoredInstant score : tpa.movingSums) {
            //if you find an exact
            if (score.time().equals(time) || score.time().isAfter(time)) {
                return score.score();
            }
        }
        throw new AssertionError("Should never get here.");
    }

//
//    private boolean meetsAirspaceRequirement(AirborneEvent summary) {
//
//        return (filterByAirspace)
//            ? summary.isInsideAirspace()
//            : true;  //ALL events make the airspace requirement if we aren't applying an airspace requirement
//    }
}
