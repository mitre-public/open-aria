package org.mitre.openaria.smoothing;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;

public class HasSmallTraversalRegionTest {


    @Test
    public void Test_FromLargeTraversal_ReturnFalse() {

        Track track = new SimpleTrack(points(
            34.010, -85.010,
            34.011, -85.011,
            34.012, -85.012,
            34.013, -85.013,
            34.014, -85.014
        ) // ~5 distinct regions when scaling factor is 1e3
        );
        HasSmallTraversalRegion<Track> test = new HasSmallTraversalRegion<>(4, 1e3);

        assertFalse(test.test(track), "Large traversals should not be flagged");
    }

    @Test
    public void Test_FromSmallTraversal_ReturnTrue() {

        Track track = new SimpleTrack(points(
            44.0010, -87.0010,
            44.0010, -87.0011,
            44.0011, -87.0011,
            44.00105, -87.00105,
            44.001103, -87.00108
        ) // ~4 distinct regions when scaling factor is 1e4
        );
        HasSmallTraversalRegion<Track> test = new HasSmallTraversalRegion<>(4, 1e4);

        assertTrue(test.test(track), "Small traversals should be flagged");
    }

    @Test
    public void Test_FromSinglePointTrack_ReturnTrue() {

        Track track = new SimpleTrack(points(34.012, -85.012));
        HasSmallTraversalRegion<Track> test = new HasSmallTraversalRegion<>();

        assertTrue(test.test(track), "Single-point tracks have small traversal by definition");
    }

    @Test
    public void Test_FromEmptyTrack_ReturnTrue() {

        Track track = MutableTrack.of(Collections.emptyList());
        HasSmallTraversalRegion<Track> test = new HasSmallTraversalRegion<>();

        assertTrue(test.test(track), "Empty tracks have small traversal by contract");
    }


    private Collection<Point> points(double... latlonlatlon) {

        Collection<Point> pts = new ArrayList<>(latlonlatlon.length / 2);

        Instant t = Instant.now();
        for (int i = 0; i < latlonlatlon.length - 1; i++) {
            pts.add(
                Point.builder().latLong(latlonlatlon[i], latlonlatlon[i + 1]).time(t)
                    .build()
            );
            t = t.plusMillis(1000);
        }
        return pts;
    }
}