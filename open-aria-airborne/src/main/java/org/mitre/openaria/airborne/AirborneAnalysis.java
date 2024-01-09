package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static java.lang.Math.max;
import static java.util.Arrays.copyOf;
import static java.util.stream.Collectors.toCollection;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;

import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.SeparationTimeSeries;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.TimeWindow;

import com.google.common.collect.Streams;

/**
 * An AirborneAnalysis packages up all the information gathered to score Airborne events. The
 * purpose of this object is permit detailed "deep dive" analysis of individual events.
 */
public class AirborneAnalysis {

    private final TimeWindow timeWindow;

    private final int n;

    private final Instant[] times;

    private final Distance[] trueLateralSeparation;

    private final Distance[] trueVerticalSeparation;

    private final Duration[] estTimeToCpa;

    private final Distance[] estLateralSepAtCpa;

    private final Distance[] estVerticalSepAtCpa;

    private final double[] score;

    public AirborneAnalysis(TrackPair trackPair) {
        checkNotNull(trackPair);

        ArrayList<SeparationPrediction> predictions = createSeparationPredictions(trackPair);

        this.n = predictions.size();
        this.timeWindow = TimeWindow.of(
            predictions.get(0).time(),
            predictions.get(n - 1).time()
        );

        this.times = new Instant[n];
        this.trueLateralSeparation = new Distance[n];
        this.trueVerticalSeparation = new Distance[n];
        this.estTimeToCpa = new Duration[n];
        this.estLateralSepAtCpa = new Distance[n];
        this.estVerticalSepAtCpa = new Distance[n];
        this.score = new double[n];

        SeparationTimeSeries sts = trackPair.separationInfo();

        for (int i = 0; i < predictions.size(); i++) {
            SeparationPrediction prediction = predictions.get(i);
            times[i] = prediction.time();
            trueLateralSeparation[i] = sts.horizontalSeparationAt(times[i]);
            trueVerticalSeparation[i] = sts.verticalSeparationAt(times[i]);
            estTimeToCpa[i] = prediction.timeUntilCpa;
            estLateralSepAtCpa[i] = prediction.lateralDistanceAtCpa;
            estVerticalSepAtCpa[i] = prediction.verticalSeparationAtCpa;
            score[i] = prediction.immediateScore();
        }
    }

    /**
     * Create a truncated copy of an AirborneAnalysis that only contains a subset of the data
     * in the original AirborneAnalysis
     */
    private AirborneAnalysis(AirborneAnalysis truncateMe, int start, int end) {
        checkArgument(0 <= start && start < truncateMe.n);
        checkArgument(0 < end && end <= truncateMe.n);
        checkArgument(start < end);

        this.n = end - start;
        this.timeWindow = TimeWindow.of(
            truncateMe.times[start],
            truncateMe.times[end - 1]
        );

        this.times = new Instant[n];
        this.trueLateralSeparation = new Distance[n];
        this.trueVerticalSeparation = new Distance[n];
        this.estTimeToCpa = new Duration[n];
        this.estLateralSepAtCpa = new Distance[n];
        this.estVerticalSepAtCpa = new Distance[n];
        this.score = new double[n];

        System.arraycopy(truncateMe.times, start, this.times, 0, n);
        System.arraycopy(truncateMe.trueLateralSeparation, start, this.trueLateralSeparation, 0, n);
        System.arraycopy(truncateMe.trueVerticalSeparation, start, this.trueVerticalSeparation, 0, n);
        System.arraycopy(truncateMe.estTimeToCpa, start, this.estTimeToCpa, 0, n);
        System.arraycopy(truncateMe.estLateralSepAtCpa, start, this.estLateralSepAtCpa, 0, n);
        System.arraycopy(truncateMe.estVerticalSepAtCpa, start, this.estVerticalSepAtCpa, 0, n);
        System.arraycopy(truncateMe.score, start, this.score, 0, n);
    }

    /**
     * Create a truncated copy of the input AirborneAnalysis that only contains data once an
     * aircraft has pierced a separation radius.
     */
    private AirborneAnalysis(AirborneAnalysis fullRecord, Distance lateralSepRadius) {
        this(
            fullRecord,
            fullRecord.firstIndexWithin(lateralSepRadius),
            fullRecord.lastIndexWithin(lateralSepRadius)
        );
    }

    /**
     * @return a truncated copy of the input AirborneAnalysis that only contains data once an
     *     aircraft has pierced a separation radius.
     */
    public AirborneAnalysis truncate(Distance lateralSepRadius) {
        return new AirborneAnalysis(this, lateralSepRadius);
    }

    /** @return the first index when the lateral separation is less than some threshold. */
    private int firstIndexWithin(Distance lateralDist) {
        for (int i = 0; i < n; i++) {
            if (trueLateralSeparation[i].isLessThanOrEqualTo(lateralDist)) {
                return i;
            }
        }
        return 0;
    }

    /** @return the last index when the lateral separation is less than some threshold. */
    private int lastIndexWithin(Distance lateralDist) {
        for (int i = n - 1; i >= 0; i--) {
            if (trueLateralSeparation[i].isLessThanOrEqualTo(lateralDist)) {
                return i;
            }
        }
        return n;
    }

    public TreeMap<Instant, Distance> estLateralSepAtCpa() {
        TreeMap<Instant, Distance> map = newTreeMap();

        for (int i = 0; i < times.length; i++) {
            map.put(times[i], estLateralSepAtCpa[i]);
        }
        return map;
    }

    public TreeMap<Instant, Distance> estVerticalSepAtCpa() {
        TreeMap<Instant, Distance> map = newTreeMap();

        for (int i = 0; i < times.length; i++) {
            map.put(times[i], estVerticalSepAtCpa[i]);
        }
        return map;
    }

    public TreeMap<Instant, Duration> timeToCpa() {
        TreeMap<Instant, Duration> map = newTreeMap();

        for (int i = 0; i < times.length; i++) {
            map.put(times[i], estTimeToCpa[i]);
        }
        return map;
    }

    public TimeWindow timeWindow() {
        return timeWindow;
    }

    public int n() {
        return n;
    }

    public Instant[] times() {
        return copyOf(times, times.length);
    }

    public Distance[] trueLateralSeparations() {
        return copyOf(trueLateralSeparation, trueLateralSeparation.length);
    }

    public Distance[] trueVerticalSeparations() {
        return copyOf(trueVerticalSeparation, trueVerticalSeparation.length);
    }

    public Duration[] estTimeToCpas() {
        return copyOf(estTimeToCpa, estTimeToCpa.length);
    }

    public Distance[] estLateralSepAtCpas() {
        return copyOf(estLateralSepAtCpa, estLateralSepAtCpa.length);
    }

    public Distance[] estVerticalSepAtCpas() {
        return copyOf(estVerticalSepAtCpa, estVerticalSepAtCpa.length);
    }

    public double[] scores() {
        return copyOf(score, score.length);
    }


    ArrayList<ScoredInstant> computeMovingSums() {
        ArrayList<ScoredInstant> movingSums = newArrayList();
        for (int i = 0; i < n; i++) {
            double score_0 = score[i]; //now
            double score_1 = score[max(0, i - 1)]; //1 time step back
            double score_2 = score[max(0, i - 2)]; //2 time steps back
            double score_3 = score[max(0, i - 3)]; //3 time steps back

            double score = score_0 + score_1 + score_2 + score_3;

            movingSums.add(new ScoredInstant(score, this.times()[i]));
        }
        return movingSums;
    }

    SerializableAnalysis serializedForm() {
        return SerializableAnalysis.of(this);
    }

    private static ArrayList<SeparationPrediction> createSeparationPredictions(TrackPair trackPair) {

        SeparationTimeSeries separationStats = trackPair.separationInfo();

        //do an separation prediction analysis at each of these moments in time
        ArrayList<SeparationPrediction> predictions = Streams.stream(separationStats.times())
            .map(instant -> new SeparationPrediction(trackPair, instant))
            .collect(toCollection(ArrayList::new));

        return predictions;
    }
}
