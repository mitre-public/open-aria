package org.mitre.openaria.airborne.metrics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.StreamingTimeSorter.StreamIntegritySummarizer;
import org.mitre.openaria.core.formats.nop.Facility;
import org.mitre.openaria.system.StreamingKpi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A KpiLogger collects "heartbeat" information from Airborne ARIA KPIs.
 *
 * <p>At each execution of this Runnable the KpiLogger harvests a "snapshot" of important data
 * (e.g.
 * How many points were processed, How many Events were found, How many points were dropped, etc.)
 * The recent change in these important data streams is computed, bundled into JsonWritable objects,
 * and then emitted to external targets (e.g. Flat Log Files, Kafka Topics, Databases, etc.).
 */
public class KpiLogger implements Runnable {

    //The sources of log data, all KPIs must be defined at construction time.
    private final Map<Facility, StreamingKpi<AirbornePairConsumer>> dataSources;

    //Where Log information gets written to...(after getting converted to JSON Strings)
    private final Consumer<String> logDestination;

    //Values retained from the last time this logger was executed
    private final Map<Facility, KpiSnapshot> priors;

    //The "name" of the computer that is running the Airborne KPI
    private final String hostIdentifier;

    public KpiLogger(String hostIdentifier, Map<Facility, StreamingKpi<AirbornePairConsumer>> logThese, Consumer<String> toHere) {
        checkNotNull(hostIdentifier);
        checkNotNull(logThese);
        checkNotNull(toHere);
        this.hostIdentifier = hostIdentifier;
        this.dataSources = unmodifiableMap(newHashMap(logThese));
        this.logDestination = toHere;
        this.priors = harvestCurrentState();
    }

    private Map<Facility, KpiSnapshot> harvestCurrentState() {
        Map<Facility, KpiSnapshot> returnMe = newHashMap();
        for (Facility key : dataSources.keySet()) {
            returnMe.put(key, new KpiSnapshot(hostIdentifier, key, dataSources.get(key)));
        }
        return returnMe;
    }

    @Override
    public void run() {

        Map<Facility, KpiSnapshot> currentData = harvestCurrentState();

        //compute changes in the data (bucket change by "type")
        List<StatsDeltas> recentFindings = recentFindings(currentData);
        List<PointSequenceDelta> recentSeqAudits = recentSequenceAudit(currentData);

        //emit "recent change heartbeat" to loggers
        recentFindings.stream().forEach((findingDelta) -> logDestination.accept(findingDelta.asJson()));
        recentSeqAudits.stream().forEach((seqDelta) -> logDestination.accept(seqDelta.asJson()));

        this.priors.clear();
        this.priors.putAll(currentData);
    }

    private List<PointSequenceDelta> recentSequenceAudit(Map<Facility, KpiSnapshot> currentData) {
        //Compute a PointSequenceDelta for each dataSource, then remove the delta's that have no data to report.
        return dataSources.keySet().stream()
            .map(facility -> new PointSequenceDelta(currentData.get(facility), priors.get(facility)))
            .filter(latency -> latency.hasDataToReport())
            .collect(Collectors.toList());
    }

    private List<StatsDeltas> recentFindings(Map<Facility, KpiSnapshot> currentData) {
        //Compute a StatsDelta for each dataSource, then remove the delta's that have no data to report.
        return dataSources.keySet().stream()
            .map(facility -> new StatsDeltas(currentData.get(facility), priors.get(facility)))
            .filter(latency -> latency.hasDataToReport())
            .collect(Collectors.toList());
    }

    static long NO_TIME_VALUE = -9999;

    /** Captures all important data at a fixed point in time. */
    static class KpiSnapshot {

        final String hostId;
        final Facility facility;
        final int numEvents;
        final long numPoints;
        final long numTracks;
        final long numTrackPairs;

        final long numDroppedPoints;
        final long totalDelayInMs;
        final long curPtSeqTimeEpochSec;
        final long harvestTimeEpochSec;

        KpiSnapshot(String hostId, Facility facility, StreamingKpi<AirbornePairConsumer> kpi) {
            this.hostId = hostId;
            this.facility = facility;
            this.numEvents = kpi.coreLogic().getEventSummarizer().eventCounts;
            this.numPoints = kpi.numPointsProcessed();
            this.numTracks = kpi.numTracksProcessed();
            this.numTrackPairs = kpi.numTrackPairsProcessed();

            StreamIntegritySummarizer sis = kpi.pointSorter().integritySummarizer();
            this.numDroppedPoints = sis.droppedCount();
            this.totalDelayInMs = sis.totalDelayOfDroppedRecordsInMs();

            //extract the time of the point data flowing into our KPI (after stream sorting)
            Instant curPtStreamTime = kpi.pointSorter().timeSeenByTarget();
            this.curPtSeqTimeEpochSec = (isNull(curPtStreamTime))
                ? NO_TIME_VALUE
                : curPtStreamTime.getEpochSecond();

            this.harvestTimeEpochSec = Instant.now().getEpochSecond();
        }
    }

    static Gson GSON_FOR_STATS_DELTAS = new GsonBuilder().create();

    /*
     * This class is automatically converted to Json and emitted to logs.
     *
     * {"logType":"FINDINGS","time":"2021-02-22T14:50:15Z","facility":"A80","events":6,"points":88768,"tracks":2573,"trackPairs":9519}
     */
    static class StatsDeltas implements JsonWritable {

        String logType = "FINDINGS";
        String hostId;
        String time;
        Facility facility;
        int events;
        long points;
        long tracks;
        long trackPairs;

        private StatsDeltas(KpiSnapshot newState, KpiSnapshot prior) {
            checkArgument(newState.facility.equals(prior.facility));
            this.hostId = newState.hostId;
            this.time = Instant.ofEpochSecond(prior.harvestTimeEpochSec).toString();
            this.facility = newState.facility;
            this.events = newState.numEvents - prior.numEvents;
            this.points = newState.numPoints - prior.numPoints;
            this.tracks = newState.numTracks - prior.numTracks;
            this.trackPairs = newState.numTrackPairs - prior.numTrackPairs;
        }

        @Override
        public String asJson() {
            String json = GSON_FOR_STATS_DELTAS.toJson(this);
            return checkNotNull(json, "Gson produced null when attempting to convert an object to JSON");
        }

        boolean hasDataToReport() {
            return points > 0
                || tracks > 0
                || trackPairs > 0;
        }
    }

    /*
     * This class is automatically converted to Json and emitted to logs.
     *
     * {"logType":"SEQ_AUDIT","facility":"A80","points":88768,"droppedPoints":0,"ptTimeChangeSec":4032,"avgLatencyDroppedPtsSec":0,"fracPtsDropped":0.0}
     */
    static class PointSequenceDelta implements JsonWritable {

        String logType = "SEQ_AUDIT";
        String hostId;
        String time;
        Facility facility;

        long points;
        long ptTimeChangeSec; //time difference between data being processed before, and after delta.
        long droppedPoints;
        int avgLatencyDroppedPtsSec;
        double fracPtsDropped;

        PointSequenceDelta(KpiSnapshot newState, KpiSnapshot prior) {
            checkArgument(newState.facility.equals(prior.facility));
            this.hostId = newState.hostId;
            this.time = Instant.ofEpochSecond(prior.harvestTimeEpochSec).toString();
            this.facility = newState.facility;
            this.points = newState.numPoints - prior.numPoints;

            this.ptTimeChangeSec = (prior.curPtSeqTimeEpochSec == NO_TIME_VALUE)
                ? 0 //better to say 0 then some huge value that equates to "Now - Jan 1st 1970"
                : newState.curPtSeqTimeEpochSec - prior.curPtSeqTimeEpochSec;

            this.droppedPoints = newState.numDroppedPoints - prior.numDroppedPoints;
            long delayDeltaInSec = (newState.totalDelayInMs - prior.totalDelayInMs) / 1000;
            this.avgLatencyDroppedPtsSec = (droppedPoints == 0)
                ? 0
                : (int) (delayDeltaInSec / droppedPoints);

            this.fracPtsDropped = (points == 0)
                ? 0
                : ((double) droppedPoints) / ((double) points);
        }

        @Override
        public String asJson() {
            String json = GSON_FOR_STATS_DELTAS.toJson(this);
            return checkNotNull(json, "Gson produced null when attempting to convert an object to JSON");
        }

        boolean hasDataToReport() {
            return points > 0;
        }
    }
}