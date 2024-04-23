
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointField;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;


public class MutableSmootherTest {

    @Test
    public void testCombinedSmoothing() {

        DataCleaner<MutableTrack> speedFixer = new FillMissingSpeeds();
        DataCleaner<MutableTrack> beaconSetter = new BeaconCodeSetter();

        //Get a TRACK cleaner from two MUTABLETRACK cleaners
        DataCleaner<Track> combinedSmoother = MutableSmoother.of(
            beaconSetter,
            speedFixer
        );

        Track testTrack = testTrack();
        Track smoothedTrack = combinedSmoother.clean(testTrack).get();

        //verify the speed fixer AND the beaconSetter were applied
        for (Point point : smoothedTrack.points()) {
            assertNotNull(point.speedInKnots());
            assertEquals(point.beaconActual(), "1234");
        }
    }

    class BeaconCodeSetter implements DataCleaner<MutableTrack> {

        @Override
        public Optional<MutableTrack> clean(MutableTrack data) {
            for (MutablePoint point : data.points()) {
                point.set(PointField.BEACON_ACTUAL, "1234");
            }
            return Optional.of(data);
        }
    }

    /*
     * This track is Immutable, it also has no speed data and no beacon codes
     */
    public Track testTrack() {

        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        Point p1 = Point.builder()
            .time(EPOCH)
            .latLong(position)
            .build();

        Point p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(position.projectOut(0.0, 5.0 * nmPerSec))
            .build();

        Point p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(position.projectOut(0.0, 10.0 * nmPerSec))
            .build();

        return Track.of(newArrayList(p1, p2, p3));
    }

}
