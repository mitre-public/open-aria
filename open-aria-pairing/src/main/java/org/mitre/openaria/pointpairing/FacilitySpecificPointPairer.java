
package org.mitre.openaria.pointpairing;

import java.time.Duration;
import java.util.function.Consumer;

import org.mitre.openaria.core.FacilitySpecificDataIngestor;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.collect.DistanceMetric;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class FacilitySpecificPointPairer {

    private final double PAIRING_THRESHOLD = 1000.0;

    FacilitySpecificDataIngestor ingestor;

    Consumer<Pair<Point, Point>> outputMechanism;

    public FacilitySpecificPointPairer(String dirOfManyNopFiles, Consumer<Pair<Point, Point>> outputMechanism) {
        this.ingestor = new FacilitySpecificDataIngestor(dirOfManyNopFiles);
        this.outputMechanism = outputMechanism;
    }

    public void findPointPairs(Facility facility) {

        PointPairFinder pairFinder = new PointPairFinder(
            Duration.ofSeconds(13),
            getDistanceMetric(),
            PAIRING_THRESHOLD,
            outputMechanism
        );

        ingestor.processPointsFrom(facility, pairFinder);
    }

    private DistanceMetric<Point> getDistanceMetric() {

        double TIME_IN_MILLISEC_COEF = 1.0;
        double DISTANCE_IN_FEET_COEF = 1.0;

        return new PointDistanceMetric(
            TIME_IN_MILLISEC_COEF,
            DISTANCE_IN_FEET_COEF
        );
    }
}
