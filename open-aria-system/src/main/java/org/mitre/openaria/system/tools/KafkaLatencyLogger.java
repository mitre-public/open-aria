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

import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.openaria.core.formats.nop.Facility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A KafkaLatencyLogger collects "heartbeat" information about the latency in Kafka's Point level
 * data feed.
 * <p>
 * At each execution of this Runnable the KafkaLatencyLogger harvests a "snapshot" of current
 * latency metrics (e.g. How many points were processed? What is the total delay between data
 * creation within Kafka and data delivery to this application? And what is the total delay between
 * data creation in Kafka and the event actually occurring?)
 * <p>
 * The change in these important data streams is computed, bundled into a JsonWritable object, and
 * then emitted to external targets (e.g. Flat Log Files, Kafka Topics, Databases, etc.).
 *
 * @param <T> the type of key in the mapping to {@link KafkaLatencyCollector}s, e.g.
 *            {@link Facility}
 */
public class KafkaLatencyLogger<T> implements Runnable {

    //The sources of log data, all KPIs must be defined at construction time.
    private final Map<T, KafkaLatencyCollector> dataSources;

    //Where Log information gets written to...(after getting converted to JSON Strings)
    private final Consumer<String> logDestination;

    //Values retained from the last time this logger was executed
    private final Map<T, LatencySnapshot<T>> priors;

    //The "name" of the computer that is running the Airborne KPI
    private final String hostIdentifier;

    public KafkaLatencyLogger(String hostIdentifier, Map<T, KafkaLatencyCollector> logThese, Consumer<String> toHere) {
        checkNotNull(hostIdentifier);
        checkNotNull(logThese);
        checkNotNull(toHere);
        this.hostIdentifier = hostIdentifier;
        this.dataSources = unmodifiableMap(newHashMap(logThese));
        this.logDestination = toHere;
        this.priors = harvestCurrentState();
    }

    private Map<T, LatencySnapshot<T>> harvestCurrentState() {
        Map<T, LatencySnapshot<T>> returnMe = newHashMap();
        for (T key : dataSources.keySet()) {
            returnMe.put(key, new LatencySnapshot<>(hostIdentifier, key, dataSources.get(key)));
        }
        return returnMe;
    }

    @Override
    public void run() {

        Map<T, LatencySnapshot<T>> currentData = harvestCurrentState();

        //compute changes in the data (bucket change by "type")
        List<LatencyDeltas<T>> recentChanges = recentFindings(currentData);

        //emit "recent change heartbeat" to loggers
        recentChanges.stream().forEach((findingDelta) -> logDestination.accept(findingDelta.asJson()));

        this.priors.clear();
        this.priors.putAll(currentData);
    }

    private List<LatencyDeltas<T>> recentFindings(Map<T, LatencySnapshot<T>> currentData) {
        //Compute a LatencyDeltas for each dataSource, then remove the delta's that have no data to report.
        return dataSources.keySet().stream()
            .map(facility -> new LatencyDeltas<>(currentData.get(facility), priors.get(facility)))
            .filter(latency -> latency.hasDataToReport())
            .collect(Collectors.toList());
    }

    /** Captures immutable latency metrics from a fixed point in time. */
    static class LatencySnapshot<T> {

        final T facility;

        final String hostId;
        final long numPoints;
        final long totalConsumeLatencyInMs;
        final long totalUploadLatencyInMs;
        final long harvestTimeEpochSec;

        LatencySnapshot(String hostId, T facility, KafkaLatencyCollector latencyCollector) {
            this.facility = facility;
            this.hostId = hostId;
            this.numPoints = latencyCollector.curPointCount();
            this.totalConsumeLatencyInMs = latencyCollector.totalConsumeLatencyMilliSec();
            this.totalUploadLatencyInMs = latencyCollector.totalUploadLatencyMilliSec();
            this.harvestTimeEpochSec = Instant.now().getEpochSecond();
        }
    }

    private static final Gson GSON_FOR_STATS_DELTAS = new GsonBuilder().create();

    /*
     * This class is automatically converted to Json and emitted to logs as text.
     */
    static class LatencyDeltas<T> implements JsonWritable {

        String logType = "LATENCY";
        T facility;
        String hostId;
        String time;
        long points;
        long avgConsumeLatencyMs;
        long avgUploadLatencyMs;

        private LatencyDeltas(LatencySnapshot<T> newState, LatencySnapshot<T> prior) {
            checkArgument(newState.facility.equals(prior.facility));
            this.facility = newState.facility;
            this.hostId = newState.hostId;
            this.time = Instant.ofEpochSecond(prior.harvestTimeEpochSec).toString();
            this.points = newState.numPoints - prior.numPoints;

            long consumeDeltaInMs = newState.totalConsumeLatencyInMs - prior.totalConsumeLatencyInMs;
            long uploadDeltaInMs = newState.totalUploadLatencyInMs - prior.totalUploadLatencyInMs;

            this.avgConsumeLatencyMs = (points == 0) ? 0 : consumeDeltaInMs / points;
            this.avgUploadLatencyMs = (points == 0) ? 0 : uploadDeltaInMs / points;
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