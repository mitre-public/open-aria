
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newLinkedList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class DistanceDownSamplerTest {


    @Test
    public void testTrackWithPause() {

        Track<String> track = trackWithPause();

        Track<String> cleanedTrack = (new DistanceDownSampler<String>()).clean(track).get();

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
    private Track<String> trackWithPause() {

        LinkedList<Point<String>> points = newLinkedList();

        Point<String> startPoint = Point.<String>builder()
            .latLong(50.0, 50.0)
            .time(EPOCH)
            .build();

        points.add(startPoint);

        addMovingPoints(points);
        addStagnantPoints(points);
        addMovingPoints(points);

        return Track.of(points);
    }

    private void addMovingPoints(LinkedList<Point<String>> points) {
        int i = 0;
        while (i < 300) {
            Point<String> lastPoint = points.getLast();
            Point<String> newPoint = Point.<String>builder()
                .time(lastPoint.time().plusSeconds(1))
                .latLong(lastPoint.latLong().projectOut(45.0, 1.0 / 60.0)) //point moves 1/60th of a mile
                .build();

            points.add(newPoint);
            i++;
        }
    }

    private void addStagnantPoints(LinkedList<Point<String>> points) {
        int i = 0;
        while (i < 300) {
            Point<String> lastPoint = points.getLast();
            Point<String> newPoint = Point.<String>builder()
                .time(lastPoint.time().plusSeconds(1))
                .latLong(lastPoint.latLong()) //point stays at the same place
                .build();

            points.add(newPoint);
            i++;
        }
    }

}
