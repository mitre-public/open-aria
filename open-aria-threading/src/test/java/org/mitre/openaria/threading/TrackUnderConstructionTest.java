
package org.mitre.openaria.threading;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;

public class TrackUnderConstructionTest {

    @Test
    public void testLastPoint() {

        Point firstPoint = (new PointBuilder()).time(EPOCH.minusSeconds(5)).build();
        Point secondPoint = (new PointBuilder()).time(EPOCH).build();

        TrackUnderConstruction tip = new TrackUnderConstruction(firstPoint);
        tip.addPoint(secondPoint);

        //the "last point" should be the oldest point added
        assertEquals(EPOCH, tip.timeOfLatestPoint());
    }

    @Test
    public void testTimeSince() {

        Point firstPoint = Point.builder().time(Instant.now()).build();

        TrackUnderConstruction tip = new TrackUnderConstruction(firstPoint);

        //the "last point" should be the oldest point added
        assertEquals(
            Duration.ofHours(1),
            tip.timeSince(firstPoint.time().plus(Duration.ofHours(1)))
        );

    }
}
