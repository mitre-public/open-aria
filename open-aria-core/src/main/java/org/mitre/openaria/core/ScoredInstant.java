/*
 * NOTICE:
 * This is the copyright work of The MITRE Corporation, and was produced for the
 * U. S. Government under Contract Number DTFAWA-10-C-00080, and is subject to
 * Federal Aviation Administration Acquisition Management System Clause 3.5-13,
 * Rights In Data-General, Alt. III and Alt. IV (Oct. 1996).
 *
 * No other use other than that granted to the U. S. Government, or to those
 * acting on behalf of the U. S. Government, under that Clause is authorized
 * without the express written permission of The MITRE Corporation. For further
 * information, please contact The MITRE Corporation, Contracts Management
 * Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.
 *
 * Copyright 2021 The MITRE Corporation. All Rights Reserved.
 */
package org.mitre.openaria.core;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;

import org.mitre.openaria.core.utils.InstantJsonSerde.InstantJsonDeserializer;
import org.mitre.openaria.core.utils.InstantJsonSerde.InstantJsonSerializer;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.out.JsonWritable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A ScoredInstant represents a moment in time that has a "risk-score".
 * <p>
 * A ScoredInstant implements Comparable so that you can easily identify the Time with the highest
 * risk (which is usually the lowest score).
 */
public final class ScoredInstant implements Comparable<ScoredInstant>, HasTime, JsonWritable {

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantJsonSerializer())
        .registerTypeAdapter(Instant.class, new InstantJsonDeserializer())
        .create();

    private final double eventScore;
    private final Instant eventTime;

    public ScoredInstant(double score, Instant time) {
        this.eventScore = score;
        this.eventTime = requireNonNull(time);
    }

    public static ScoredInstant of(double score, Instant time) {
        return new ScoredInstant(score, time);
    }

    public double score() {
        return eventScore;
    }

    @Override
    public Instant time() {
        return eventTime;
    }

    @Override
    public int compareTo(ScoredInstant o) {
        int scoreResult = Double.compare(this.score(), o.score());
        if (scoreResult != 0) {
            return scoreResult;
        }
        return time().compareTo(o.time());
    }

    /**
     * Serialized example: <pre>
     *     {"eventScore":12.12,"eventTime":{"epochMilli":1617025242521,"utc":"2021-03-29T13:40:42.521Z"}}
     * </pre>
     *
     * @return the JSON-serialized form of this instance
     */
    @Override
    public String asJson() {
        return GSON.toJson(this);
    }

    public static ScoredInstant fromJson(String json) {
        return GSON.fromJson(json, ScoredInstant.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScoredInstant that = (ScoredInstant) o;
        return Double.compare(that.eventScore, eventScore) == 0 && Objects.equals(eventTime, that.eventTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventScore, eventTime);
    }

    @Override
    public String toString() {
        return asJson();
    }
}
