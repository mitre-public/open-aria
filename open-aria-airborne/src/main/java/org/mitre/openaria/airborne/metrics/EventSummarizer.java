package org.mitre.openaria.airborne.metrics;

import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.mitre.openaria.airborne.AirborneEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * An EventSummarizer organizes AirborneEvents so that they can be summarized by Date.
 *
 * <p>Multiple EventSummarizers (that track different data streams) can be combined to help form
 * aggregated logs.
 */
public class EventSummarizer implements Consumer<AirborneEvent> {

    //contains Daily Event Summary data broken down by Date (i.e. "2020-01-13"") and Facility
    Table<String, Integer, DailyMetrics> summaries;

    /** The function Assigns AirborneEvents to a "common group" for aggregate reporting. */
    private final Function<AirborneEvent, Integer> eventGrouper;

    int eventCounts;

    public EventSummarizer(Function<AirborneEvent, Integer> eventGrouper) {
        requireNonNull(eventGrouper);
        this.summaries = HashBasedTable.create();
        this.eventGrouper = eventGrouper;
        this.eventCounts = 0;
    }

    public EventSummarizer() {
        this(e -> 0);
    }

//    /**
//     * Incorporate all the summary information from the other EventSummarizer into this
//     * EventSummarizer.
//     *
//     * @param other Another EventSummarizer (that usually collected data from a completely distinct
//     *              data stream)
//     */
//    public void ingestAll(EventSummarizer<GROUP> other) {
//        for (Table.Cell<String, GROUP, DailyMetrics> cell : other.summaries.cellSet()) {
//            String date = cell.getRowKey();
//            GROUP group = cell.getColumnKey();
//            DailyMetrics metrics = cell.getValue();
//
//            DailyMetrics prior = summaries.get(date, group);
//
//            if (prior == null) {
//                summaries.put(date, group, metrics);
//            } else {
//                summaries.put(date, group, combine(prior, metrics));
//            }
//        }
//    }

    @Override
    public void accept(AirborneEvent event) {
        eventCounts++;

        String date = utcDateAsString(event.time());
        Integer group = eventGrouper.apply(event);

        DailyMetrics prior = summaries.get(date, group);

        if (prior == null) {
            summaries.put(date, group, new DailyMetrics(event));
        } else {
            summaries.put(date, group, new DailyMetrics(prior, event));
        }
    }

//    public Map<GROUP, DailyMetrics> getSummariesFor(Instant time) {
//        checkNotNull(time);
//        return getSummariesFor(utcDateAsString(time));
//    }

//    public Map<GROUP, DailyMetrics> getSummariesFor(String date) {
//        checkNotNull(date);
//        verifyYearMonthDayFormat(date);
//        return summaries.row(date);
//    }

    public Collection<DailyMetrics> getAllSummaries() {
        return summaries.values();
    }

    /** @return The Set of Dates ("YYYY-MM-DD") that have data. */
    public Set<String> dateKeys() {
        return summaries.rowKeySet();
    }

    public int eventCount() {
        return this.eventCounts;
    }
}
