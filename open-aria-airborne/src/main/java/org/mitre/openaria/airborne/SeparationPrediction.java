

package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.padStart;
import static org.apache.commons.math3.util.FastMath.pow;
import static org.mitre.openaria.airborne.AirborneUtils.isEstablishedAtAltitude;
import static org.mitre.caasd.commons.Time.asZTimeString;
import static org.mitre.caasd.commons.Time.theDuration;

import java.time.Duration;
import java.time.Instant;

import org.mitre.openaria.core.ClosestPointOfApproach;
import org.mitre.openaria.core.PointPair;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;

/**
 * A AirborneSeparationPredication takes two instantaneous aircraft trajectories and predicts: (1)
 * What the minimum lateral separation between the aircraft will be, (2) when this minimum
 * separation will occur, and (3) what the vertical separation between be at this time.
 *
 * <p>These AirborneSeparationPredication can be used to: (A) describe the interaction between two
 * aircraft, (B) graphically show how that interaction evolves over time (assuming you have a
 * sequence of SeparationPredication objects)
 */
public class SeparationPrediction {

    static final CoefficentSet ABOVE_18000_FT = new CoefficentSet(
        1.0 / 0.25, //.25 NM of Horizontal Separation equals 1 score unit
        1.0 / 250.0, //250 ft of Vetical Separation equals 1 score unit
        1.0 / 30_000.0, //30_000 MS of Time Separation equals 1 score unit
        4.0
    );
    static final CoefficentSet BELOW_18000_FT = new CoefficentSet(
        1.0 / 0.25, //.25 NM of Horizontal Separation equals 1 score unit
        1.0 / 150.0, //150 ft of Vetical Separation equals 1 score unit
        1.0 / 30_000.0, //30_000 MS of Time Separation equals 1 score unit
        4.0
    );

    static final Duration LEVEL_FLIGHT_REQUIREMENT = Duration.ofSeconds(30);

    //prints the immediate scores if necessary
    public static boolean VERBOSE = false;

    private final TrackPair tracks;

    private final Instant time;

    PointPair points;

    Duration timeUntilCpa;

    Distance lateralDistanceAtCpa;

    Distance verticalSeparationAtCpa;

    Speed verticalClosureRate;

    Speed lateralClosureRate;

    private final double immediateScore;

    public SeparationPrediction(TrackPair trackPair, Instant time) {
        checkNotNull(trackPair);
        checkNotNull(time);

        try {

            this.tracks = trackPair;
            this.time = time;
            this.points = trackPair.interpolatedPointsAt(time);

            ClosestPointOfApproach cpa = points.closestPointOfApproach();

            this.timeUntilCpa = cpa.timeUntilCpa();
            this.lateralDistanceAtCpa = cpa.distanceAtCpa();

            this.verticalSeparationAtCpa = trackPair.separationInfo()
                .predictedVerticalSeparation(time, timeUntilCpa);

            this.verticalClosureRate = trackPair.separationInfo().verticalClosureRateAt(time);
            this.lateralClosureRate = trackPair.separationInfo().horizontalClosureRateAt(time);

            this.immediateScore = computeImmediateScore();

        } catch (NullPointerException npe) {
            String track1 = trackPair.track1().asNop();
            String track2 = trackPair.track2().asNop();
            throw new RuntimeException(track1 + "\n" + track2 + "\n", npe);
        }
    }

    private double computeImmediateScore() {
        return computeImmediateScore(determineCoefficients());
    }

    private double computeImmediateScore(CoefficentSet coefs) {

        double timeScore = coefs.timeCoef * timeUntilCpa.toMillis();
        double horizontalScore = coefs.horizontalCoef * lateralDistanceAtCpa.inNauticalMiles();
        double verticalScore = coefs.verticalCoef * verticalSeparationAtCpa.inFeet();

        //Tweak "hypot" so that scores increase faster than a regular 2-dimensional distance calculation
        double baseScore
            = pow(timeScore, 2)
            + pow(pow(horizontalScore, 2.5) + pow(verticalScore, 2.5), 0.5);

        //if not closing vertically -- penalize the score with a multiplicative factor
        double verticalPenalty = computeVerticalPenalty(coefs);
        checkState(verticalPenalty >= 0);
        baseScore *= verticalPenalty;

        //if not closing laterally -- penalize score with a multiplicative factor
        double lateralPenalty = computeLateralPenalty(coefs);
        checkState(lateralPenalty >= 0);
        baseScore *= lateralPenalty;

        if (VERBOSE) {
            System.out.println("  score:\t" + format(baseScore)
                + "\t" + asZTimeString(time)
                + "\t" + format(timeUntilCpa)
                + "\t" + lateralDistanceAtCpa.toString(2)
                + "\t" + verticalSeparationAtCpa.toString(0));
        }
        return baseScore;
    }

    public Instant time() {
        return time;
    }

    public double immediateScore() {
        return immediateScore;
    }

    private CoefficentSet determineCoefficients() {
        return (points.avgAltitude().isGreaterThan(Distance.ofFeet(18_000)))
            ? ABOVE_18000_FT
            : BELOW_18000_FT;
    }

    private String format(double score) {
        int NUM_DECIMAL_PLACES = 3;
        return String.format("%." + NUM_DECIMAL_PLACES + "f", score);
    }

    /** Compute a MULTIPLICATIVE penalty to apply when the aircraft ARE NOT converging vertically. */
    private double computeVerticalPenalty(CoefficentSet coefs) {

        //Penalties should ALWAYS range from 1 to BIG_VALUE
        if (tracksAreBothLevel() || tracksAreDiverging()) {
            /*
             * We STOPPED using this formula because it requires an abrupt transition threshold.
             * This means flights below the threshold have no penalty and flights at the threshold
             * have a rather big penalty (at least 2.6 when using 400ft threshold or 3.8 when using
             * the 700ft threshold)
             */
            //return 1.0 + coefs.verticalCoef * points.altitudeDelta().inFeet();

            /*
             * We swapped to this formula so that (1) we could eliminate the abrupt transition from
             * no penalty to big penalty (2)
             */
            return 1.0 + pow(points.altitudeDelta().inFeet() / 700.0, 2.5);
        } else {
            //AIRCRAFT ARE CONVERING OR FLYING LEVEL (WITHOUT A CUSHION OF VERTICAL SEPARATION)
            return 1.0;
        }
    }

    private boolean tracksAreBothLevel() {
        boolean trackIsLevel_1 = isEstablishedAtAltitude(tracks.track1(), time, LEVEL_FLIGHT_REQUIREMENT);
        boolean trackIsLevel_2 = isEstablishedAtAltitude(tracks.track2(), time, LEVEL_FLIGHT_REQUIREMENT);

        return trackIsLevel_1 && trackIsLevel_2;
    }

    private boolean tracksAreDiverging() {
        return verticalClosureRate.inFeetPerSecond() < 0.0;
    }

    private double computeLateralPenalty(CoefficentSet coefs) {

        //if closing very slowly....OR OUTRIGHT DIVERGING  (3 knots is about a fast walking pace)
        if (lateralClosureRate.inKnots() <= 3.0) {
            //from 0-to-BIG_VALUE
            double currentLateralWeight = coefs.horizontalCoef * points.lateralDistance().inNauticalMiles();
            //add 1 to ensure the minimum penalty is 1
            return 1 + currentLateralWeight;
        } else {
            return 1.0; //no multiplicative penalty
        }
    }

    public static void setVerboseScoring(boolean v) {
        VERBOSE = v;
    }

    /**
     * Format a Duration object for easily readable score reports
     *
     * @param duration A duration
     *
     * @return An easily read String like "1m 5.123 sec" or "6.789 sec". If the input duration is
     *     longer than an hour the formating defaults to regular Duration.toString() output.
     */
    static String format(Duration duration) {
        if (theDuration(duration).isGreaterThan(Duration.ofHours(1))) {
            return duration.toString();
        }
        long min = duration.toMinutes();
        long sec = duration.getSeconds() % 60;
        long ms = duration.toMillis() % 1000;

        StringBuilder sb = new StringBuilder();
        if (min > 0) {
            sb.append(min + "m ");
        }
        return sb.append(sec + "." + padStart(Long.toString(ms), 3, '0') + " sec").toString();
    }

    /**
     * CoefficentSets are uses along with the measurements in SeparationPredication to compute a
     * risk score at a particular moment in time.
     */
    static class CoefficentSet {

        final double horizontalCoef;
        final double verticalCoef;
        final double timeCoef;
        final double nonConvergingPenalty;

        CoefficentSet(double horizontal, double vertical, double time, double nonConvergePenalty) {
            this.horizontalCoef = horizontal;
            this.verticalCoef = vertical;
            this.timeCoef = time;
            this.nonConvergingPenalty = nonConvergePenalty;
        }
    }
}
