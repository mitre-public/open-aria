
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;


public class MutableSmootherTest {

    @Test
    public void testCombinedSmoothing() {

        DataCleaner<MutableTrack> speedFixer = new FillMissingSpeeds();
        DataCleaner<MutableTrack> altitudeSetter = new AltitudeSetter();

        //Get a TRACK cleaner from two MUTABLETRACK cleaners
        DataCleaner<Track> combinedSmoother = MutableSmoother.of(
            altitudeSetter,
            speedFixer
        );

        Track testTrack = testTrack();
        Track smoothedTrack = combinedSmoother.clean(testTrack).get();

        //verify the speed fixer AND the altitudeSetter were applied
        for (Point point : smoothedTrack.points()) {
            assertNotNull(point.speedInKnots());
            assertThat(point.altitude(), is(Distance.ofFeet(1000)));
        }
    }

    class AltitudeSetter implements DataCleaner<MutableTrack> {

        @Override
        public Optional<MutableTrack> clean(MutableTrack data) {

            TreeSet<Point> fixedPoints = data.points().stream()
                .map(pt -> Point.builder(pt).butAltitude(Distance.ofFeet(1000)).build())
                .collect(Collectors.toCollection(TreeSet::new));

            return Optional.of(MutableTrack.of(fixedPoints));
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
