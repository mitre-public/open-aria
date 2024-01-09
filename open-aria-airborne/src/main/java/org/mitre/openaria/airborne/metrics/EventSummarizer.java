package org.mitre.openaria.airborne.metrics;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.airborne.metrics.DailyMetrics.combine;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;
import static org.mitre.caasd.commons.YyyyMmDd.verifyYearMonthDayFormat;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.caasd.commons.parsing.nop.Facility;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * An EventSummarizer organizes AirborneEvents so that they can be summarized by Date and Facility.
 *
 * <p>Multiple EventSummarizers (that track different data streams) can be combined to help form
 * aggregated logs.
 */
public class EventSummarizer implements Consumer<AirborneEvent> {

    //contains Daily Event Summary data broken down by Date (i.e. "2020-01-13"") and Facility
    Table<String, Facility, DailyMetrics> summaries;

    int eventCounts;

    public EventSummarizer() {
        this.summaries = HashBasedTable.create();
        this.eventCounts = 0;
    }

    /**
     * Incorporate all the summary information from the other EventSummarizer into this
     * EventSummarizer.
     *
     * @param other Another EventSummarizer (that usually collected data from a completely distinct
     *              data stream)
     */
    public void ingestAll(EventSummarizer other) {
        for (Table.Cell<String, Facility, DailyMetrics> cell : other.summaries.cellSet()) {
            String date = cell.getRowKey();
            Facility facility = cell.getColumnKey();
            DailyMetrics metrics = cell.getValue();

            DailyMetrics prior = summaries.get(date, facility);

            if (prior == null) {
                summaries.put(date, facility, metrics);
            } else {
                summaries.put(date, facility, combine(prior, metrics));
            }
        }
    }

    @Override
    public void accept(AirborneEvent event) {
        eventCounts++;

        String date = utcDateAsString(event.time());
        Facility facility = event.nopFacility();

        DailyMetrics prior = summaries.get(date, facility);

        if (prior == null) {
            summaries.put(date, facility, new DailyMetrics(event));
        } else {
            summaries.put(date, facility, new DailyMetrics(prior, event));
        }
    }

    public Map<Facility, DailyMetrics> getSummariesFor(Instant time) {
        checkNotNull(time);
        return getSummariesFor(utcDateAsString(time));
    }

    public Map<Facility, DailyMetrics> getSummariesFor(String date) {
        checkNotNull(date);
        verifyYearMonthDayFormat(date);
        return summaries.row(date);
    }

    public Map<String, DailyMetrics> getSummariesFor(Facility facility) {
        checkNotNull(facility);
        return summaries.column(facility);
    }

    public Collection<DailyMetrics> getAllSummaries() {
        return summaries.values();
    }

    /** @return The Set of Dates ("YYYY-MM-DD") that have data. */
    public Set<String> dateKeys() {
        return summaries.rowKeySet();
    }

    public Set<Facility> facilityKeys() {
        return summaries.columnKeySet();
    }

    public int eventCount() {
        return this.eventCounts;
    }
}
