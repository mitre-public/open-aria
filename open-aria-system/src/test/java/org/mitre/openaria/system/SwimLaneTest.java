package org.mitre.openaria.system;

import static java.time.Instant.EPOCH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.openaria.system.StreamingKpi.IN_MEMORY_SORT_BUFFER_SEC;
import static org.mitre.openaria.system.SwimLane.OVER_FLOW_FILEPREFIX;

import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SwimLaneTest {


    /** Create a dummy StreamingKpi. */
    public static StreamingKpi<Consumer<Track>> simpleStreamingKpi() {
        Properties props = new Properties();
        props.setProperty(IN_MEMORY_SORT_BUFFER_SEC, "600");

        Consumer<Track> trackConsumer = (track) -> {};

        return StreamingKpi.trackBasedKpi(
            trackConsumer,
            props
        );

    }


    //THIS FAILS BECAUSE SWIM LANES CAN DROP POINTS
    @Disabled
    @Test
    public void swimLaneDoesNotDropPoints() throws InterruptedException {

        //Create a SwimLane that can hold exactly 1 point
        SwimLane lane = new SwimLane(simpleStreamingKpi(), 1);

        Point p1 = Point.builder().latLong(0.0, 1.0).time(EPOCH).build();
        Point p2 = Point.builder().latLong(0.0, 1.0).time(EPOCH.plusSeconds(1)).build();

        lane.offerToQueue(p1);
        lane.offerToQueue(p2);

        //THIS FAILS!! -- FLAW -- THE CURRENT SWIMLANE CAN DROP POINT
        assertThat(lane.queueSize(), is(2));
    }

    @Test
    public void swimLaneEmitsBreadcrumbsOnOverflow() {

        SwimLane lane = new SwimLane(simpleStreamingKpi(), 1);
        Point point = Point.builder().latLong(0.0, 1.0).time(EPOCH).build();

        //these files should be made when the swim lane rejects data because the queue was full
        File warn1 = new File(OVER_FLOW_FILEPREFIX + 1 + ".txt");
        File warn10 = new File(OVER_FLOW_FILEPREFIX + 10 + ".txt");
        File warn100 = new File(OVER_FLOW_FILEPREFIX + 100 + ".txt");

        int exceptionCount = 0;

        assertThat(warn1.exists(), is(false));
        assertThat(warn10.exists(), is(false));
        assertThat(warn100.exists(), is(false));
        assertThat(exceptionCount, is(0));

        lane.offerToQueue(point); //accept 1st point

        assertThat(warn1.exists(), is(false));
        assertThat(warn10.exists(), is(false));
        assertThat(warn100.exists(), is(false));
        assertThat(exceptionCount, is(0));

        try {
            lane.offerToQueue(point); //drop this point
        } catch (IllegalStateException ise) {
            exceptionCount++;
        }

        assertThat(warn1.exists(), is(true));
        assertThat(warn10.exists(), is(false));
        assertThat(warn100.exists(), is(false));
        assertThat(exceptionCount, is(1));

        for (int i = 0; i < 10; i++) {
            try {
                lane.offerToQueue(point);  //drop 10 more points
            } catch (IllegalStateException ise) {
                exceptionCount++;
            }
        }

        assertThat(warn1.exists(), is(true));
        assertThat(warn10.exists(), is(true));
        assertThat(warn100.exists(), is(false));
        assertThat(exceptionCount, is(2));

        for (int i = 0; i < 100; i++) {
            try {
                lane.offerToQueue(point);  //drop 100 more points
            } catch (IllegalStateException ise) {
                exceptionCount++;
            }
        }

        assertThat(warn1.exists(), is(true));
        assertThat(warn10.exists(), is(true));
        assertThat(warn100.exists(), is(true));
        assertThat(exceptionCount, is(3));

        warn1.delete();
        warn10.delete();
        warn100.delete();
    }
}
