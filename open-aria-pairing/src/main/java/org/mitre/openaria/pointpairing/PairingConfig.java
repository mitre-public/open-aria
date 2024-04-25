package org.mitre.openaria.pointpairing;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Duration;

import org.mitre.caasd.commons.Spherical;

/**
 * A PairingConfig requires 3 inputs {pairingDistanceInNm, timeCoef, and distCoef}.
 *
 * <p>These 3 inputs are used to derived the 2 inputs that a PointPairFinder requires: the
 * DistanceMetric itself and the pairing threshold.
 */
public class PairingConfig {

    private final Duration timeWindow;
    private final double trackPairingDistanceInNM;
    private final double timeCoef;
    private final double distCoef;

    public PairingConfig(Duration timeWindow, double trackPairingDistanceInNM, double timeCoef, double distCoef) {
        requireNonNull(timeWindow);
        checkArgument(timeWindow.toMillis() > 100);
        checkArgument(trackPairingDistanceInNM > 0);
        this.timeWindow = timeWindow;
        this.trackPairingDistanceInNM = trackPairingDistanceInNM;
        this.timeCoef = timeCoef;
        this.distCoef = distCoef;
    }

    public PairingConfig(Duration timeWindow, double trackPairingDistanceInNM) {
        this(timeWindow, trackPairingDistanceInNM, 1.0, 1.0);
    }

    /**
     * @return The default asProperties that pairs any two aircraft that come within 10 Nautical
     *     Miles of each other.
     */
    public static PairingConfig standardPairingProperties() {
        return new PairingConfig(Duration.ofSeconds(13), 10.0);
    }

    /** @return The amount of data kept to do point pairing with. */
    public Duration timeWindow() {
        return timeWindow;
    }

    public double trackPairingDistanceInNM() {
        return trackPairingDistanceInNM;
    }

    public double timeCoef() {
        return timeCoef;
    }

    public double distCoef() {
        return distCoef;
    }

    public FlatDistanceMetric distMetric() {
        return new FlatDistanceMetric(timeCoef(), distCoef());
    }

    public double pairingThreshold() {
        /*
         * We use 7000 because this equals 7 seconds (in milliseconds). Radar hits are normally
         * updated every 13 seconds or less. Thus, we know any two aircraft will have radar hits
         * within 6.5 seconds of each other. 6500 is rounded up to 7000 because...why not.
         */
        final double timeComponent = timeCoef() * 7_000;
        final double distComponent = distCoef() * trackPairingDistanceInNM() * Spherical.feetPerNM();

        final double pairingThreshold = timeComponent + distComponent;

        return pairingThreshold;
    }
}
