package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.caasd.commons.Time.asZTimeString;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.caasd.commons.Histogram;
import org.mitre.caasd.commons.parsing.nop.Facility;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Doubles;

/**
 * An EventStatisticsCollector is designed to help audit a stream of AirborneEvent (which is usually
 * pulled from Kafka)
 *
 * <p>An EventStatisticsCollector collects: Event counts (per facility), Event Scores (per
 * facility)
 */
public class EventStatisticsCollector implements Consumer<AirborneEvent> {

    private final EnumMultiset<Facility> eventCounts;

    private final EnumMap<Facility, Multiset<Double>> eventScoresByFacility;

    private final EnumMap<Facility, ArrayList<Instant>> eventTimesByFacility;

    private final EnumMap<Facility, Long> totalBytesByFacility;

    private final Instant now;

    public EventStatisticsCollector() {
        this.eventCounts = EnumMultiset.create(Facility.class);
        this.eventScoresByFacility = new EnumMap<>(Facility.class);
        this.eventTimesByFacility = new EnumMap<>(Facility.class);
        this.totalBytesByFacility = new EnumMap<>(Facility.class);
        for (Facility facility : Facility.values()) {
            eventScoresByFacility.put(facility, HashMultiset.create());
            eventTimesByFacility.put(facility, newArrayList());
            totalBytesByFacility.put(facility, 0L);
        }

        this.now = Instant.now(); //The age of events are measured against this instant
    }

    @Override
    public void accept(AirborneEvent event) {

        Facility facility = event.nopFacility();
        final byte[] asBytes = event.asJson().getBytes(); //contains EventRecord AND trackdata

        eventCounts.add(facility);
        eventScoresByFacility.get(facility).add(event.score());
        eventTimesByFacility.get(facility).add(event.time());
        totalBytesByFacility.put(facility, totalBytesByFacility.get(facility) + asBytes.length);
    }

    public Histogram scoreHistogramFor(Facility facility) {
        return makeScoreHistogram(eventScoresByFacility.get(facility));
    }

    private Histogram makeScoreHistogram(Multiset<Double> scores) {
        List<Double> list = Lists.newArrayList(scores.iterator());
        double[] data = Doubles.toArray(list);

        double max = (data.length == 0)
            ? 0
            : Doubles.max(data);
        int numColumns = (int) (max / 5.0) + 1;

        return Histogram.builder()
            .min(0)
            .max(numColumns * 5)
            .numColumns(numColumns)
            .fromRawData(data)
            .build();
    }

    public Histogram timeHistogramFor(Facility facility) {
        return makeTimeHistogram(eventTimesByFacility.get(facility));
    }

    private Histogram makeTimeHistogram(Collection<Instant> times) {

        //convert all the event times to "ages in hours"
        List<Double> agesInHours = newArrayList();
        for (Instant time : times) {
            Duration age = Duration.between(now, time).abs();
            agesInHours.add((double) age.toHours());
        }

        double[] data = Doubles.toArray(agesInHours);

        double max = (data.length == 0)
            ? 0
            : Doubles.max(data);
        double numHoursPerHistBar = 24.0;
        int numColumns = (int) (max / numHoursPerHistBar) + 1;

        Histogram hist = Histogram.builder()
            .min(0)
            .max(numColumns * numHoursPerHistBar)
            .numColumns(numColumns)
            .fromRawData(data)
            .build();

        return hist;
    }

    public String makeFacilityReport(Facility facility) {

        return makeReport(
            eventTimesByFacility.get(facility),
            eventScoresByFacility.get(facility),
            totalBytesByFacility.get(facility),
            facility.toString()
        );
    }

    /** Collect data from all Facilities, create a single report for this data. */
    public String makeSummaryReport() {

        List<Instant> allTimes = newArrayList();

        for (ArrayList<Instant> value : eventTimesByFacility.values()) {
            allTimes.addAll(value);
        }

        Multiset<Double> allScores = HashMultiset.create();
        for (Multiset<Double> value : eventScoresByFacility.values()) {
            allScores.addAll(value);
        }

        long totalBytes = 0;
        for (Long value : totalBytesByFacility.values()) {
            totalBytes += value;
        }

        return makeReport(
            allTimes,
            allScores,
            totalBytes,
            "TOTALS"
        ).toString();

    }

    private String makeReport(List<Instant> eventTimes, Multiset<Double> eventScores, long totalBytes, String name) {

        //summarize by facility:
        StringBuilder facilitySummary = new StringBuilder("Event count by facility:\n");
        for (Facility facility : Facility.values()) {
            facilitySummary.append("  " + facility + ": " + eventCounts.count(facility)).append("\n");
        }

        Instant minTime = eventTimes.stream().min(Instant::compareTo).orElse(null);
        Instant maxTime = eventTimes.stream().max(Instant::compareTo).orElse(null);

        final String minTimeMessage = (minTime == null)
            ? "  Min Event Time: N/A\n"
            : "  Min Event Time: " + utcDateAsString(minTime) + " " + asZTimeString(minTime) + "\n";
        final String maxTimeMessage = (maxTime == null)
            ? "  Max Event Time: N/A\n"
            : "  Max Event Time: " + utcDateAsString(maxTime) + " " + asZTimeString(maxTime) + "\n";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("  Number Airborne Events Found: ").append(eventTimes.size()).append("\n");
        sb.append("  Total Bytes: ").append(totalBytes).append("\n");
        sb.append(minTimeMessage);
        sb.append(maxTimeMessage);
        sb.append(facilitySummary.toString()).append("\n");
        sb.append("Event Age (in hours) Histogram:").append("\n");
        sb.append(makeTimeHistogram(eventTimes).toString(0)).append("\n");
        sb.append("Event Score Histogram:").append("\n");
        sb.append(makeScoreHistogram(eventScores).toString(0)).append("\n");
        sb.append("\n");

        return sb.toString();
    }
}
