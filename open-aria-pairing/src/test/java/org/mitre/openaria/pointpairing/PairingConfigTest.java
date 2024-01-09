
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mitre.openaria.pointpairing.PairingConfig.standardPairingProperties;


import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Spherical;

public class PairingConfigTest {

    static Duration timeWindow = Duration.ofSeconds(13);

    @Test
    public void testConstructor() {
        PairingConfig pp = new PairingConfig(timeWindow, 10, 2, 1);

        double TOLERANCE = 0.00001;

        assertEquals(10.0, pp.trackPairingDistanceInNM(), TOLERANCE);
        assertEquals(2.0, pp.timeCoef(), TOLERANCE);
        assertEquals(1.0, pp.distCoef(), TOLERANCE);
    }


    @Test
    public void testDerivedPairThresholdComputation() {

        PairingConfig stdProps = standardPairingProperties();

        double TOLERANCE = 0.0001;

        assertEquals(
            stdProps.pairingThreshold(),
            7000.0 + 10.0 * Spherical.feetPerNM(),
            TOLERANCE
        );
    }

    @Test
    public void testDerivedPairThresholdReflectsTimeCoef() {

        double TOLERANCE = 0.0001;

        PairingConfig noTimeProps = new PairingConfig(timeWindow, 10, 0, 1.0);

        assertEquals(
            noTimeProps.pairingThreshold(),
            10.0 * Spherical.feetPerNM(),
            TOLERANCE
        );
    }

    @Test
    public void testDerivedPairThresholdReflectsDistCoef() {

        double TOLERANCE = 0.0001;

        PairingConfig noDistProps = new PairingConfig(timeWindow, 10, 1, 0);

        assertEquals(
            noDistProps.pairingThreshold(),
            7000.0,
            TOLERANCE
        );
    }

    @Test
    public void testDerivedPairThresholdReflectsDistanceInNM() {

        double TOLERANCE = 0.0001;

        PairingConfig noDistProps = new PairingConfig(timeWindow, 5, 1, 1);

        assertEquals(
            noDistProps.pairingThreshold(),
            7000.0 + 5.0 * Spherical.feetPerNM(),
            TOLERANCE
        );
    }

    @Test
    public void testDerivedDistanceMetricReflectsCoefs() {

        PairingConfig pp = new PairingConfig(timeWindow, 5, 2, 1);

        FlatDistanceMetric metric = pp.distMetric();

        double TOLERANCE = 0.0001;

        assertEquals(1.0, metric.distanceCoef(), TOLERANCE);
        assertEquals(2.0, metric.timeCoef(), TOLERANCE);
    }
}
