
package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;


public class SimpleTrackTest {

    @Test
    public void testTrackConstructor() {

        /*
         * We want to be able to build a SimpleTrack from a Collection of any type of objects that
         * implement the Point interface
         *
         * The point here is that the "points" variable is a List<T> where T implements Point.
         */
        List<BasicPointImplementation> points = newArrayList(new BasicPointImplementation());
        SimpleTrack track = new SimpleTrack(points);

        assertTrue(true, "This test proves the code above compiles properly");

        for (Point point : track.points()) {
            /*
             * Notice, this "for-each" loop iterates of Point objects even though the input was a
             * list of BasicPointImplementation objects.
             */
        }
    }

    @Test
    public void testTrackConstructorWithListContainingTwoPointTypes() {

        /*
         * We want to be able to build a SimpleTrack from a Collection of any type of objects that
         * implement the Point interface
         *
         * The point here is that the "points" list contains two different Point implementations.
         */
        List<Point> points = newArrayList(
            new BasicPointImplementation(),
            new BasicPointImplementation_2()
        );
        SimpleTrack track = new SimpleTrack(points);

        assertTrue(true,"This test proves the code above compiles properly");
    }

}
