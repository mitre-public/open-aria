package org.mitre.openaria.airborne.metrics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Double.isNaN;
import static java.lang.Math.min;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;
import static org.mitre.caasd.commons.YyyyMmDd.verifyYearMonthDayFormat;

import java.time.Instant;
import java.util.Arrays;

import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.caasd.commons.parsing.nop.Facility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DailyMetrics implements JsonWritable {

    /* The converter is static to allow reuse. Creating the Gson using reflection is expensive. */
    private static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();

    //Histogram measure scores from 0 to 100.  20 bars of width 5
    public static final int HISTOGRAM_BIN_WIDTH = 5;
    public static final int NUM_HISTOGRAM_BINS = 20;

    /** Date defined like: yyyy-mm-dd */
    private final String date;
    private final Facility facility;
    private final int eventCount;
    private final double avgEventScore;
    private final int[] histogram;

    /** No-argument constructor, should only be used by automated deserialization tools */
    public DailyMetrics() {
        this.date = null;
        this.facility = null;
        this.eventCount = 0;
        this.avgEventScore = 0;
        this.histogram = new int[NUM_HISTOGRAM_BINS];

        //NOTE: when this constructor is call the result DailyMetrics object is in an invalid state
    }

    /**
     * Create a DailyMetrics object that "summarizes" exactly one event
     *
     * @param record The sole event (must have NOP Facility, event time, and event score
     */
    public DailyMetrics(AirborneEvent record) {
        this(record.nopFacility(), record.time(), record.score());
    }

    /**
     * Create a DailyMetrics object that "summarizes" exactly one event
     *
     * @param nopFacility The facility where the event occurred
     * @param eventTime   The time the event occurred
     * @param eventScore  The score of the event
     */
    public DailyMetrics(Facility nopFacility, Instant eventTime, double eventScore) {
        checkNotNull(nopFacility);
        checkNotNull(eventTime);
        checkArgument(eventScore > 0);

        this.date = utcDateAsString(eventTime); //save date as YYYY-MM-DD formated String
        this.facility = nopFacility;
        this.eventCount = 1;
        this.avgEventScore = eventScore;
        this.histogram = new int[NUM_HISTOGRAM_BINS];
        histogram[histogramBinIndexFor(eventScore)]++;
    }

    /**
     * Create a brand new DailyMetrics object that merges the prior state and the new event.
     *
     * @param prior    A DailyMetrics object
     * @param newEvent Another event from the same Facility and Day
     */
    public DailyMetrics(DailyMetrics prior, AirborneEvent newEvent) {
        checkNotNull(prior);
        checkNotNull(newEvent);

        prior.validate();

        //require same facility and date
        checkNotNull(newEvent.nopFacility());
        checkNotNull(newEvent.time());
        checkArgument(prior.facility.equals(newEvent.nopFacility()));
        checkArgument(prior.date.equals(utcDateAsString(newEvent.time())));

        this.facility = prior.facility;
        this.date = prior.date;

        this.eventCount = prior.eventCount + 1;
        this.avgEventScore = (prior.sumEventScores() + newEvent.score()) / ((double) eventCount);

        this.histogram = Arrays.copyOf(prior.histogram, prior.histogram.length);
        this.histogram[histogramBinIndexFor(newEvent.score())]++;
    }

    /**
     * Combine two DailyMetrics object into a single value. Combining DailyMetrics objects from
     * different facilities or days will work but the Facility or Date field will be lost because
     * there is no longer a single "correct date" or "correct facility".
     */
    private DailyMetrics(DailyMetrics one, DailyMetrics two) {
        checkNotNull(one);
        checkNotNull(two);

        //date is null if either input has a null value OR the dates don't match
        this.date = ((one.date == null || two.date == null) || !one.date.equals(two.date))
            ? null
            : one.date;

        //facility is null if either input has a null value OR the facility don't match
        this.facility = ((one.facility == null || two.facility == null) || !one.facility.equals(two.facility))
            ? null
            : one.facility;

        this.eventCount = one.eventCount + two.eventCount;
        this.avgEventScore = ((one.avgEventScore * one.eventCount) + (two.avgEventScore * two.eventCount)) / ((double) this.eventCount);
        this.histogram = new int[NUM_HISTOGRAM_BINS];
        for (int i = 0; i < this.histogram.length; i++) {
            this.histogram[i] = one.histogram[i] + two.histogram[i];
        }
    }

    public static DailyMetrics combine(DailyMetrics one, DailyMetrics two) {
        return new DailyMetrics(one, two);
    }

    /**
     * Throw an exception if this object is not properly built. This method helps ensure object
     * built with reflection using JSON parsing tools are still properly configured.
     */
    public void validate() {
        checkNotNull(date, "date cannot be null");
        verifyYearMonthDayFormat(date);  //ensure YYYY-MM-DD

        checkNotNull(facility, "facility cannot be null");

        checkState(eventCount >= 0, "eventCount must be non-negative");
        checkState(avgEventScore >= 0, "avgEventScore must be non-negative");

        //histogram must exist, have length N, all entries must be non-negative
        checkNotNull(histogram, "The histogram array cannot be null");
        checkState(histogram.length == NUM_HISTOGRAM_BINS, "The histogram must have " + NUM_HISTOGRAM_BINS + " entries");
        int sum = 0;
        for (int i : histogram) {
            checkState(i >= 0, "All histogram entries must be non-negative");
            sum += i;
        }
        checkState(eventCount == sum, "eventCount does not equal the sum of the histogram entries");
    }

    public String date() {
        return date;
    }

    public Facility facility() {
        return facility;
    }

    public int eventCount() {
        return eventCount;
    }

    public double avgScore() {
        return avgEventScore;
    }

    public int[] histogram() {
        return histogram;
    }

    /** Create unique key from concatenation of date and facility */
    public String key() {
        return this.date() + "--" + this.facility();
    }

    private double sumEventScores() {
        return eventCount * avgEventScore;
    }

    private int histogramBinIndexFor(double eventScore) {
        checkArgument(!isNaN(eventScore), "The eventScore cannot be NaN");
        checkArgument(eventScore >= 0, "The event score must be non-negative");
        //event scores OVER the histograms intended range are clamped to be in the max bin
        return min(
            NUM_HISTOGRAM_BINS - 1,
            (int) (eventScore / HISTOGRAM_BIN_WIDTH)
        );
    }

    @Override
    public String asJson() {
        //use the static Gson to prevent frequent use of expensive java.lang.reflect calls
        String json = GSON_CONVERTER.toJson(this);

        // Adjust the JSON string to remove all breaklines between entries in the histogram array
        int startIndex = json.indexOf('[') + 1;
        int endIndex = json.indexOf(']');

        String histString = json.substring(startIndex, endIndex);
        String histStringWithoutBreaklines = histString.replace("\n   ", "").trim();

        return json.replace(histString, histStringWithoutBreaklines);
    }

    public static DailyMetrics parseJson(String json) {
        DailyMetrics objGeneratedFromJson = GSON_CONVERTER.fromJson(json, DailyMetrics.class);
        objGeneratedFromJson.validate();
        return objGeneratedFromJson;
    }
}
