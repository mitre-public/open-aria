package org.mitre.openaria.airborne;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;

import org.mitre.openaria.core.ClosestPointOfApproach;
import org.mitre.openaria.core.PointPair;
import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.SeparationTimeSeries;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.out.JsonWritable;

/**
 * A Snapshot is an "instantaneous view" of the dynamics between two aircraft at an important moment
 * in time.
 */
public class Snapshot implements JsonWritable {

    final String timestamp;
    final long epochMsTime;
    final double score;
    final double trueVerticalFt;
    final double trueLateralNm;
    final int angleDelta;

    final double vertClosureRateFtPerMin;
    final double lateralClosureRateKt;

    @org.apache.avro.reflect.Nullable
    final Long estTimeToCpaMs;
    @org.apache.avro.reflect.Nullable
    final Double estVerticalAtCpaFt;
    @org.apache.avro.reflect.Nullable
    final Double estLateralAtCpaNm;

    private Snapshot(Builder bldr) {
        this.timestamp = bldr.time.toString();
        this.epochMsTime = bldr.time.toEpochMilli();
        this.score = bldr.score;
        this.trueVerticalFt = bldr.trueVerticalFt;
        this.trueLateralNm = bldr.trueLateralNm;
        this.angleDelta = bldr.angleDelta;
        this.vertClosureRateFtPerMin = bldr.vertClosureRateFtPerMin;
        this.lateralClosureRateKt = bldr.lateralClosureRateKt;
        this.estTimeToCpaMs = bldr.estTimeToCpaMs;
        this.estVerticalAtCpaFt = bldr.estVerticalAtCpaFt;
        this.estLateralAtCpaNm = bldr.estLateralAtCpaNm;
    }

    public static Snapshot extractSnapshot(TrackPair pair, Instant time, Double score) {
        requireNonNull(pair);
        requireNonNull(time);

        //A snapshot cannot be made because data for at least one aircraft is missing
        if (!pair.overlapContains(time)) {
            return null;
        }

        PointPair points = pair.interpolatedPointsAt(time);
        SeparationTimeSeries sts = pair.separationInfo();

        return newBuilder()
            .time(time)
            .score(score)
            .trueVertical(points.altitudeDelta())
            .trueLateral(points.lateralDistance())
            .angleDelta(points.angleDelta())
            .verticalClosureRate(sts.verticalClosureRateAt(time))
            .lateralClosureRate(points.horizontalClosure())
            .build();
    }

    public static Snapshot extractSnapshotWithCpa(TrackPair pair, ScoredInstant si) {
        requireNonNull(pair);
        requireNonNull(si);

        Instant time = si.time();

        //A snapshot cannot be made because data for at least one aircraft is missing
        if (!pair.overlapContains(time)) {
            return null;
        }

        PointPair points = pair.interpolatedPointsAt(time);
        SeparationTimeSeries sts = pair.separationInfo();
        ClosestPointOfApproach cpa = points.closestPointOfApproach();

        return newBuilder()
            .time(time)
            .score(si.score())
            .trueVertical(points.altitudeDelta())
            .trueLateral(points.lateralDistance())
            .angleDelta(points.angleDelta())
            .verticalClosureRate(sts.verticalClosureRateAt(time))
            .lateralClosureRate(points.horizontalClosure())
            .estVerticalAtCpaFt(sts.predictedVerticalSeparation(time, cpa.timeUntilCpa()))
            .estLateralAtCpaNm(cpa.distanceAtCpa())
            .timeToCpa(cpa.timeUntilCpa())
            .build();
    }

    public String timestamp() {
        return timestamp;
    }

    public long epochMsTime() {
        return epochMsTime;
    }

    public double score() {
        return score;
    }

    public double trueVerticalFt() {
        return trueVerticalFt;
    }

    public double trueLateralNm() {
        return trueLateralNm;
    }

    public int angleDelta() {
        return angleDelta;
    }

    public double vertClosureRateFtPerMin() {
        return vertClosureRateFtPerMin;
    }

    public double lateralClosureRateKt() {
        return lateralClosureRateKt;
    }

    @Nullable //only reported for "atEventTime" Snapshot
    public Long estTimeToCpaMs() {
        return estTimeToCpaMs;
    }

    @Nullable //only reported for "atEventTime" Snapshot
    public Double estVerticalAtCpaFt() {
        return estVerticalAtCpaFt;
    }

    @Nullable //only reported for "atEventTime" Snapshot
    public Double estLateralAtCpaNm() {
        return estLateralAtCpaNm;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        Instant time;
        double score;
        double trueVerticalFt;
        double trueLateralNm;
        Integer angleDelta;

        double vertClosureRateFtPerMin;
        double lateralClosureRateKt;

        Long estTimeToCpaMs;
        Double estVerticalAtCpaFt;
        Double estLateralAtCpaNm;

        public Builder time(Instant time) {
            this.time = time;
            return this;
        }

        public Builder trueVertical(Distance verticalSep) {
            this.trueVerticalFt = verticalSep.inFeet();
            return this;
        }

        public Builder trueLateral(Distance lateralSep) {
            this.trueLateralNm = lateralSep.inNauticalMiles();
            return this;
        }

        public Builder angleDelta(Course delta) {
            this.angleDelta = (int) delta.abs().inDegrees();
            return this;
        }

        private Builder verticalClosureRate(Speed verticalClosureRateAt) {
            this.vertClosureRateFtPerMin = verticalClosureRateAt.inFeetPerMinutes();
            return this;
        }

        private Builder lateralClosureRate(Speed lateralClosureRate) {
            this.lateralClosureRateKt = lateralClosureRate.inKnots();
            return this;
        }

        private Builder timeToCpa(Duration timeUntilCpa) {
            this.estTimeToCpaMs = timeUntilCpa.toMillis();
            return this;
        }

        private Builder estVerticalAtCpaFt(Distance estVerticalAtCpa) {
            this.estVerticalAtCpaFt = estVerticalAtCpa.inFeet();
            return this;
        }

        private Builder estLateralAtCpaNm(Distance estLateralAtCpa) {
            this.estLateralAtCpaNm = estLateralAtCpa.inNauticalMiles();
            return this;
        }

        private Builder score(Double score) {
            this.score = score;
            return this;
        }

        public Snapshot build() {
            return new Snapshot(this);
        }
    }
}
