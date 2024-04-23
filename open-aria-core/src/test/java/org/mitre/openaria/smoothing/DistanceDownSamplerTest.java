
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newLinkedList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;

import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class DistanceDownSamplerTest {

    @Test
    public void testTrackWithPause() {

        Track track = trackWithPause();

        MutableTrack cleanedTrack = (new DistanceDownSampler()).clean(track.mutableCopy()).get();

        int numRemovedPoints = track.size() - cleanedTrack.size();

        assertEquals(
            901, track.size(), "1 seed point + 300 moving + 300 stagnant + 300 moving"
        );
        assertEquals(
            290, numRemovedPoints, "Of the 300 stagnant points only the 10 heartbeat points should remain"
        );
    }

    /**
     * Create a Track with 1 point every second. The track contains 1 seed point, 300 points that
     * move, 300 points that stay still, and 300 more points that move.
     */
    private Track trackWithPause() {

        LinkedList<Point> points = newLinkedList();

        Point startPoint = Point.builder()
            .latLong(50.0, 50.0)
            .time(EPOCH)
            .build();

        points.add(startPoint);

        addMovingPoints(points);
        addStagnantPoints(points);
        addMovingPoints(points);

        return Track.of(points);
    }

    private void addMovingPoints(LinkedList<Point> points) {
        int i = 0;
        while (i < 300) {
            Point lastPoint = points.getLast();
            Point newPoint = Point.builder()
                .time(lastPoint.time().plusSeconds(1))
                .latLong(lastPoint.latLong().projectOut(45.0, 1.0 / 60.0)) //point moves 1/60th of a mile
                .build();

            points.add(newPoint);
            i++;
        }
    }

    private void addStagnantPoints(LinkedList<Point> points) {
        int i = 0;
        while (i < 300) {
            Point lastPoint = points.getLast();
            Point newPoint = Point.builder()
                .time(lastPoint.time().plusSeconds(1))
                .latLong(lastPoint.latLong()) //point stays at the same place
                .build();

            points.add(newPoint);
            i++;
        }
    }

}
