package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.unmodifiableMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.mitre.openaria.system.SwimLane;
import org.mitre.caasd.commons.out.JsonWritable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A SwimLaneLogger collects "heartbeat" information about the amount of data
 * that has entered and left a SwimLane's queue of data.
 * <p>
 * At each execution of this Runnable the SwimLaneLogger harvests a "snapshot"
 * of current
 * metrics (e.g. How many points were ingested? And How many points were
 * processed?)
 * <p>
 * The change in these important data streams is computed, bundled into a
 * JsonWritable object, and
 * then emitted to external targets (e.g. Flat Log Files, Kafka Topics,
 * Databases, etc).
 *
 * @param <T> the type of key in the mapping to (e.g. Facility or Airport)
 */
public class SwimLaneLogger<T> implements Runnable {

    // The sources of log data, all KPIs must be defined at construction time.
    private final Map<T, SwimLane> dataSources;

    // Where Log information gets written to..(after getting converted to JSON
    // Strings)
    private final Consumer<String> logDestination;

    // Values retained from the last time this logger was executed
    private final Map<T, BufferSnapshot<T>> priors;

    // The "name" of the computer that is running the Airborne KPI
    private final String hostIdentifier;

    public SwimLaneLogger(String hostIdentifier, Map<T, SwimLane> logThese, Consumer<String> toHere) {
        checkNotNull(hostIdentifier);
        checkNotNull(logThese);
        checkNotNull(toHere);
        this.hostIdentifier = hostIdentifier;
        this.dataSources = unmodifiableMap(newHashMap(logThese));
        this.logDestination = toHere;
        this.priors = harvestCurrentState();
    }

    private Map<T, BufferSnapshot<T>> harvestCurrentState() {
        Map<T, BufferSnapshot<T>> returnMe = newHashMap();
        for (T key : dataSources.keySet()) {
            returnMe.put(key, new BufferSnapshot<>(hostIdentifier, key, dataSources.get(key)));
        }
        return returnMe;
    }

    @Override
    public void run() {

        Map<T, BufferSnapshot<T>> currentData = harvestCurrentState();

        // compute changes in the data (bucket change by "type")
        List<BufferDeltas<T>> recentChanges = recentFindings(currentData);

        // emit "recent change heartbeat" to loggers
        recentChanges.stream().forEach((findingDelta) -> logDestination.accept(findingDelta.asJson()));

        this.priors.clear();
        this.priors.putAll(currentData);
    }

    private List<BufferDeltas<T>> recentFindings(Map<T, BufferSnapshot<T>> currentData) {
        // Compute a LatencyDeltas for each dataSource, then remove the delta's that
        // have no data to report.
        return dataSources.keySet().stream()
                .map(facility -> new BufferDeltas<>(currentData.get(facility),
                        priors.get(facility)))
                .filter(latency -> latency.hasDataToReport())
                .collect(Collectors.toList());
    }

    /** Captures immutable metrics at a fixed point in time. */
    static class BufferSnapshot<T> {

        final T facility;

        final String hostId;
        final long numPointsIngested;
        final long numPointsProcessed;
        final long harvestTimeEpochSec;
        final long curQueueSize;

        BufferSnapshot(String hostId, T facility, SwimLane lane) {
            this.facility = facility;
            this.hostId = hostId;
            this.numPointsIngested = lane.numPointsIngested();
            this.numPointsProcessed = lane.numPointsProcessed();
            this.curQueueSize = lane.queueSize();
            this.harvestTimeEpochSec = Instant.now().getEpochSecond();
        }
    }

    private static final Gson GSON_FOR_STATS_DELTAS = new GsonBuilder().create();

    /*
     * This class is automatically converted to Json and emitted to logs as text.
     */
    static class BufferDeltas<T> implements JsonWritable {

        String logType = "BUFFER";
        T facility;
        String hostId;
        String time;
        long numPointsIngested;
        long numPointsProcessed;
        long numPointsDelta;
        long curQueueSize;

        private BufferDeltas(BufferSnapshot<T> newState, BufferSnapshot<T> prior) {
            checkArgument(newState.facility.equals(prior.facility));
            this.facility = newState.facility;
            this.hostId = newState.hostId;
            this.time = Instant.ofEpochSecond(prior.harvestTimeEpochSec).toString();
            this.curQueueSize = newState.curQueueSize;
            this.numPointsIngested = newState.numPointsIngested - prior.numPointsIngested;
            this.numPointsProcessed = newState.numPointsProcessed - prior.numPointsProcessed;
            this.numPointsDelta = numPointsIngested - numPointsProcessed;
        }

        @Override
        public String asJson() {
            String json = GSON_FOR_STATS_DELTAS.toJson(this);
            return checkNotNull(json, "Gson produced null when attempting to convert an object to JSON");
        }

        boolean hasDataToReport() {
            return numPointsProcessed > 0 || numPointsIngested > 0;
        }
    }
}