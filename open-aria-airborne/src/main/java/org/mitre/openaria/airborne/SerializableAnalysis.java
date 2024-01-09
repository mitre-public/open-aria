package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.out.JsonWritable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Floats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class contains the same information available in {@link AirborneAnalysis} however this class
 * is structured to make a "nice" serialized object rather than a clean,useful API.
 */
@Immutable
public final class SerializableAnalysis implements JsonWritable {

    /**
     * The converter is static so that it can be reused. Reflection is expensive.
     *
     * <p>Be warned that any json writing that goes around this {@link Gson} instance is fully
     * capable
     * of producing different json for the same in memory instance
     */
    private static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();

    private final long[] epochMsTime;

    private final float[] trueVerticalFt;

    private final float[] trueLateralNm;

    private final long[] estTimeToCpaMs;

    private final float[] estVerticalAtCpaFt;

    private final float[] estLateralAtCpaNm;

    private final double[] score;

    @VisibleForTesting
    SerializableAnalysis(
        long[] timesUnixMs,
        float[] trueLateralSeparationNm,
        float[] trueVerticalSeparationFt,
        long[] estTimeToCpaMillis,
        float[] estLateralSepAtCpaNm,
        float[] estVerticalSepAtCpaFt,
        double[] score
    ) {
        // implicitly checks non-null
        checkSameLength(timesUnixMs, trueLateralSeparationNm, trueLateralSeparationNm, estTimeToCpaMillis, estLateralSepAtCpaNm, estVerticalSepAtCpaFt, score);
        this.epochMsTime = copyOf(timesUnixMs, timesUnixMs.length);
        this.trueLateralNm = copyOf(trueLateralSeparationNm, trueLateralSeparationNm.length);
        this.trueVerticalFt = copyOf(trueVerticalSeparationFt, trueVerticalSeparationFt.length);
        this.estTimeToCpaMs = copyOf(estTimeToCpaMillis, estTimeToCpaMillis.length);
        this.estLateralAtCpaNm = copyOf(estLateralSepAtCpaNm, estLateralSepAtCpaNm.length);
        this.estVerticalAtCpaFt = copyOf(estVerticalSepAtCpaFt, estVerticalSepAtCpaFt.length);
        this.score = copyOf(score, score.length);
    }

    @VisibleForTesting
    static void checkSameLength(Object... arrays) {
        Set<Integer> arrayLengths = java.util.Arrays.stream(arrays)
            .map(Array::getLength)
            .collect(toSet());

        checkArgument(arrayLengths.size() == 1, "Must be equally sized arrays");
    }

    private static long[] mapToEpochMilli(Instant[] instants) {
        return stream(instants).mapToLong(Instant::toEpochMilli).toArray();
    }

    private static long[] mapToMillis(Duration[] durations) {
        return stream(durations).mapToLong(Duration::toMillis).toArray();
    }

    private static float[] mapToNm(Distance[] distances) {
        return map(distances, d -> (float) d.inNauticalMiles());
    }

    private static float[] mapToFt(Distance[] distances) {
        return map(distances, d -> (float) d.inFeet());
    }

    private static float[] map(Distance[] distances, Function<Distance, Float> map) {
        return Floats.toArray(stream(distances).map(map).collect(Collectors.toList()));
    }

    public static SerializableAnalysis of(AirborneAnalysis airborneAnalysis) {
        return new SerializableAnalysis(
            mapToEpochMilli(airborneAnalysis.times()),
            mapToNm(airborneAnalysis.trueLateralSeparations()),
            mapToFt(airborneAnalysis.trueVerticalSeparations()),
            mapToMillis(airborneAnalysis.estTimeToCpas()),
            mapToNm(airborneAnalysis.estLateralSepAtCpas()),
            mapToFt(airborneAnalysis.estVerticalSepAtCpas()),
            copyOf(airborneAnalysis.scores(), airborneAnalysis.n())
        );
    }

    public long[] timesUnixMs() {
        return copyOf(epochMsTime, epochMsTime.length);
    }

    public float[] trueLateralSeparationNm() {
        return copyOf(trueLateralNm, trueLateralNm.length);
    }

    public float[] trueVerticalSeparationFt() {
        return copyOf(trueVerticalFt, trueVerticalFt.length);
    }

    public long[] getEstTimeToCpaMillis() {
        return copyOf(estTimeToCpaMs, estTimeToCpaMs.length);
    }

    public float[] estLateralSepAtCpaNm() {
        return copyOf(estLateralAtCpaNm, estLateralAtCpaNm.length);
    }

    public float[] estVerticalSepAtCpaFt() {
        return copyOf(estVerticalAtCpaFt, estVerticalAtCpaFt.length);
    }

    public double[] score() {
        return copyOf(score, score.length);
    }

    @Override
    public String asJson() {
        return GSON_CONVERTER.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SerializableAnalysis that = (SerializableAnalysis) o;
        return Arrays.equals(epochMsTime, that.epochMsTime)
            && Arrays.equals(trueLateralNm, that.trueLateralNm)
            && Arrays.equals(trueVerticalFt, that.trueVerticalFt)
            && Arrays.equals(estTimeToCpaMs, that.estTimeToCpaMs)
            && Arrays.equals(estLateralAtCpaNm, that.estLateralAtCpaNm)
            && Arrays.equals(estVerticalAtCpaFt, that.estVerticalAtCpaFt)
            && Arrays.equals(score, that.score);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(epochMsTime);
        result = 31 * result + Arrays.hashCode(trueLateralNm);
        result = 31 * result + Arrays.hashCode(trueVerticalFt);
        result = 31 * result + Arrays.hashCode(estTimeToCpaMs);
        result = 31 * result + Arrays.hashCode(estLateralAtCpaNm);
        result = 31 * result + Arrays.hashCode(estVerticalAtCpaFt);
        result = 31 * result + Arrays.hashCode(score);
        return result;
    }

    public static SerializableAnalysis parseJson(String json) {
        return GSON_CONVERTER.fromJson(json, SerializableAnalysis.class);
    }
}
