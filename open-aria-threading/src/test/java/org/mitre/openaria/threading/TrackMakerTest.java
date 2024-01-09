
package org.mitre.openaria.threading;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.CommonPoint;
import org.mitre.openaria.core.NopPoint;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;

public class TrackMakerTest {

    /*
     * Counts calls to "accept"
     */
    static class TestConsumer<Track> implements Consumer<Track> {

        int numCallsToAccept = 0;

        @Override
        public void accept(Track t) {
            numCallsToAccept++;
        }
    }

    ;

    private static CommonPoint newPoint(String trackId, Instant pointTime) {

        return (new PointBuilder())
            .trackId(trackId)
            .time(pointTime)
            .facility("AAA")
            .sensor("AAA")
            .latLong(0.0, 0.0)
            .build();
    }

    @Test
    public void testTrackClosure() {

        Duration TIME_LIMIT = Duration.ofSeconds(5);
        TestConsumer consumer = new TestConsumer<>();

        TrackMaker maker = new TrackMaker(TIME_LIMIT, consumer);

        assertTrue(
            consumer.numCallsToAccept == 0,
            "The consumer has not been access yet"
        );

        maker.accept(newPoint("track1", Instant.EPOCH));
        maker.accept(newPoint("differentTrack", Instant.EPOCH.plus(TIME_LIMIT.plusSeconds(1))));

        assertTrue(
            consumer.numCallsToAccept == 1,
            "track1 should be closed because the 2nd point is over the TIME_LIMIT"
        );
    }

    @Test
    public void testTrackClosure_atTimeLimit() {

        TestConsumer consumer = new TestConsumer();

        Duration TIME_LIMIT = Duration.ofSeconds(5);

        TrackMaker maker = new TrackMaker(TIME_LIMIT, consumer);

        assertTrue(
            consumer.numCallsToAccept == 0,
            "The consumer has not been access yet"
        );

        maker.accept(newPoint("track1", Instant.EPOCH));
        maker.accept(newPoint("differentTrack", Instant.EPOCH.plus(TIME_LIMIT)));

        assertTrue(
            consumer.numCallsToAccept == 0,
            "The track should not be closed quite yet, we are at the TIME_LIMIT, not over it"
        );
    }

    @Test
    public void testTrackClosure_multipleTracks() {

        Duration TIME_LIMIT = Duration.ofSeconds(5);
        TestConsumer consumer = new TestConsumer<>();

        TrackMaker maker = new TrackMaker(TIME_LIMIT, consumer);

        assertTrue(
            consumer.numCallsToAccept == 0,
            "The consumer has not been access yet"
        );

        maker.accept(newPoint("track1", Instant.EPOCH));
        maker.accept(newPoint("track1", Instant.EPOCH.plus(TIME_LIMIT.plusSeconds(1))));
        maker.accept(newPoint("differentTrack", Instant.EPOCH.plus(TIME_LIMIT.multipliedBy(5))));

        assertTrue(
            consumer.numCallsToAccept == 2,
            "there should be two \"track1\" tracks, both contain exactly 1 point"
        );
    }

    @Test
    public void testMakingTracksWithDistantPoints() {
        /*
         * Verify point that are "threadable" but too far apart in physical space are not threaded
         * together.
         */
        String trackId = "hello";
        Instant time1 = Instant.EPOCH;
        Instant time2 = time1.plusSeconds(3);
        Instant time3 = time2.plusSeconds(3);
        Instant time4 = time3.plusSeconds(3);
        Instant time5 = time4.plusSeconds(3);

        Point point1 = (new PointBuilder())
            .trackId(trackId).time(time1)
            .facility("AAA").sensor("AAA")
            .latLong(0.0, 0.0)
            .build();

        Point point2 = (new PointBuilder())
            .trackId(trackId).time(time2)
            .facility("AAA").sensor("AAA")
            .latLong(0.0, 0.0001)
            .build();

        Point point3 = (new PointBuilder())
            .trackId(trackId).time(time3)
            .facility("AAA").sensor("AAA")
            .latLong(0.0, 0.0002)
            .build();

        /*
         * This 4th point should not be "tracked" with the prior 3 point -- it is too far away.
         *
         * This 4th point should also prompt the publication of a 3 point track.
         */
        Point point4 = (new PointBuilder())
            .trackId(trackId).time(time4)
            .facility("AAA").sensor("AAA")
            .latLong(0.0, 1.0)
            .build();

        /*
         * This 5th point should not be "tracked" with the prior point -- it is too far away.
         *
         * This 5th point should also prompt the publication of a 1 point track.
         */
        Point point5 = (new PointBuilder())
            .trackId(trackId).time(time5)
            .facility("AAA").sensor("AAA")
            .latLong(0.0, 0.0004)
            .build();

        ConsumingArrayList<Track> aggregator = newConsumingArrayList();

        TrackMaker maker = new TrackMaker(Duration.ofMinutes(5), aggregator);
        maker.accept(point1);
        maker.accept(point2);
        maker.accept(point3);
        maker.accept(point4);
        maker.accept(point5);

        ArrayList<Track> publishedTracks = aggregator;
        assertEquals(2, publishedTracks.size());
        assertEquals(3, publishedTracks.get(0).points().size(), "The 1st track should have 3 points");
        assertEquals(1, publishedTracks.get(1).points().size(), "The 2nd track should have 1 point");
    }

    @Test
    public void testTimeRegression_instantaneous() {

        TrackMaker maker = new TrackMaker(Duration.ofMinutes(5), new TestConsumer());

        maker.accept(newPoint("track1", Instant.EPOCH));
        maker.accept(newPoint("track1", Instant.EPOCH.plusSeconds(60L * 1)));
        maker.accept(newPoint("track1", Instant.EPOCH.plusSeconds(60L * 2)));
        maker.accept(newPoint("track1", Instant.EPOCH.plusSeconds(60L * 3)));
        maker.accept(newPoint("track1", Instant.EPOCH.plusSeconds(60L * 4)));
        maker.accept(newPoint("track1", Instant.EPOCH.plusSeconds(60L * 5)));

        Point badInputPoint = newPoint("track1", Instant.EPOCH);

        /*
         * The points supplied to a TrackMaker cannot go backwards in time. Some form of input
         * filter must guarantee this.
         */
        assertThrows(
            IllegalArgumentException.class,
            () -> maker.accept(badInputPoint)
        );
    }

    @Test
    public void pointCountsAreCorrect() {

        String ONE = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        String TWO = "[RH],STARS,D21_B,03/24/2018,14:42:04.750,N518SP,C172,,5256,032,110,184,042.92457,-083.70999,3472,5256,-14.5847,42.7043,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";

        NopPoint pt1 = NopPoint.from(ONE);
        NopPoint pt2 = NopPoint.from(TWO);
        NopPoint pt2_copy = NopPoint.from(TWO);

        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();

        TrackMaker maker = new TrackMaker(trackConsumer);

        maker.accept(pt1);
        assertEquals(maker.currentPointCount(), 1);
        assertEquals(maker.numTracksUnderConstruction(), 1);
        maker.accept(pt2);
        assertEquals(maker.currentPointCount(), 2);
        assertEquals(maker.numTracksUnderConstruction(), 1);

        //the Point count should go up even when we add a duplicate Point...
        maker.accept(pt2_copy);
        assertEquals(maker.currentPointCount(), 3);
        assertEquals(maker.numTracksUnderConstruction(), 1);

        maker.flushAllTracks();

        //1 track was create
        assertEquals(1, trackConsumer.size());

        //However, the sole Track only had 2 points because the duplicate was dropped
        Track singleTrack = trackConsumer.get(0);
        assertEquals(2, singleTrack.size());
    }

    @Test
    public void flushAllTracks() {

        String ONE = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        String TWO = "[RH],STARS,D21_B,03/24/2018,14:42:04.750,N518SP,C172,,5256,032,110,184,042.92457,-083.70999,3472,5256,-14.5847,42.7043,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";

        //different Track ID (from 3472 to 3473)
        String A = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3473,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        String B = "[RH],STARS,D21_B,03/24/2018,14:42:04.750,N518SP,C172,,5256,032,110,184,042.92457,-083.70999,3473,5256,-14.5847,42.7043,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";

        TestConsumer trackConsumer = new TestConsumer();
        TrackMaker maker = new TrackMaker(trackConsumer);

        maker.accept(NopPoint.from(ONE));
        maker.accept(NopPoint.from(A));
        maker.accept(NopPoint.from(TWO));
        maker.accept(NopPoint.from(B));

        maker.flushAllTracks();

        assertEquals(0, maker.currentPointCount());
        assertEquals(0, maker.numTracksUnderConstruction());
        assertEquals(4, maker.numPointsPublished());
        assertEquals(2, maker.numTracksPublished());

        assertEquals(2, trackConsumer.numCallsToAccept);
    }


    @Test
    public void autoTrackClosureConstructorParameterIsRespected() {

        Duration maxPointDelta = Duration.ofSeconds(30);
        Duration maxTrackAge_small = Duration.ofMinutes(1); //way too small in real life
        Duration maxTrackAge_big = Duration.ofHours(2); //default value

        //5 points from the same flight that span 1m 20sec (manipulated times for testing)
        String p1 = "[RH],Center,ZDV,07-09-2019,02:49:54.000,SWA5423,B737,L,0564,380,414,229,37.3058,-101.8194,638,,,,,ZKC/21,,ZDV,,,,E0256,DEN,,IFR,,638,244060382,BWI,,380//380,,L,1,,,{RH}";
        String p2 = "[RH],Center,ZDV,07-09-2019,02:50:16.000,SWA5423,B737,L,0564,380,415,229,37.2911,-101.8403,638,,,,,ZKC/21,,ZDV,,,,E0256,DEN,,IFR,,638,244060652,BWI,,380//380,,L,1,,,{RH}";
        String p3 = "[RH],Center,ZDV,07-09-2019,02:50:39.000,SWA5423,B737,L,0564,380,415,229,37.2758,-101.8619,638,,,,,ZKC/21,,ZDV,,,,E0256,DEN,,IFR,,638,244060923,BWI,,380//380,,L,1,,,{RH}";
        String p4 = "[RH],Center,ZDV,07-09-2019,02:50:51.000,SWA5423,B737,L,0564,380,414,229,37.2606,-101.8833,638,,,,,ZKC/21,,ZDV,,,,E0256,DEN,,IFR,,638,244061193,BWI,,380//380,,L,1,,,{RH}";
        String p5 = "[RH],Center,ZDV,07-09-2019,02:51:14.000,SWA5423,B737,L,0564,380,414,229,37.2453,-101.9053,638,,,,,ZKC/21,,ZDV,,,,E0256,DEN,,IFR,,638,244061462,BWI,,380//380,,L,1,,,{RH}";

        TestConsumer smallCounter = new TestConsumer();
        TrackMaker smallTrackMaker = new TrackMaker(maxPointDelta, maxTrackAge_small, smallCounter);

        smallTrackMaker.accept(NopPoint.from(p1));
        smallTrackMaker.accept(NopPoint.from(p2));
        smallTrackMaker.accept(NopPoint.from(p3));
        smallTrackMaker.accept(NopPoint.from(p4));
        smallTrackMaker.accept(NopPoint.from(p5)); //this point should cause the track to be "closeable"

        assertThat(smallCounter.numCallsToAccept, is(1));

        //now do same thing...but with a bigger maxTrackAge
        TestConsumer bigCounter = new TestConsumer();
        TrackMaker bigTrackMaker = new TrackMaker(maxPointDelta, maxTrackAge_big, bigCounter);

        bigTrackMaker.accept(NopPoint.from(p1));
        bigTrackMaker.accept(NopPoint.from(p2));
        bigTrackMaker.accept(NopPoint.from(p3));
        bigTrackMaker.accept(NopPoint.from(p4));
        bigTrackMaker.accept(NopPoint.from(p5)); //this point should NOT cause the track to be "closeable"

        assertThat(bigCounter.numCallsToAccept, is(0)); //nothing emitted yet
    }

    @Test
    public void trackClosureAgeReflectsConstructor() {

        TrackMaker trackMaker = new TrackMaker(
            Duration.ofSeconds(30),
            Duration.ofSeconds(1234),
            new TestConsumer<>()
        );

        assertThat(trackMaker.forceTrackClosureAge(), is(Duration.ofSeconds(1234)));
    }

    @Test
    public void verifyTrackClosure_spuriousInitialPoint() {
        /*
         * This track excerpt appeared in an exception.  This test verifies the TrackMaker was
         *  working as expected (
         */

        Duration maxPointDelta = Duration.ofSeconds(45);

        //6 points from the same flight -- the first point was "too early" to be a part of the Track
        String p1 = "[RH],STARS,A90,08/28/2020,01:09:46.140,,,,1522,318,524,,42.74582,-73.62129,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String p2 = "[RH],STARS,A90,08/28/2020,01:11:10.101,,,,1522,304,529,91,42.74110,-73.34395,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String p3 = "[RH],STARS,A90,08/28/2020,01:11:14.751,,,,1522,304,528,91,42.74089,-73.32845,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String p4 = "[RH],STARS,A90,08/28/2020,01:11:19.351,,,,1522,303,526,91,42.74067,-73.31317,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String p5 = "[RH],STARS,A90,08/28/2020,01:11:28.581,,,,1522,302,524,91,42.74023,-73.28268,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String p6 = "[RH],STARS,A90,08/28/2020,01:11:33.201,,,,1522,301,524,90,42.74004,-73.26742,2600,0,,,,,,A90,,,,,,,IFR,,,,,,,,,,,,{RH}";

        ConsumingArrayList<Track> sink = new ConsumingArrayList<>();
        TrackMaker tm = new TrackMaker(maxPointDelta, sink);

        tm.accept(NopPoint.from(p1));
        tm.accept(NopPoint.from(p2)); //this 2nd point should evict the "1st track" because that track is "too stale" to accept new points
        tm.accept(NopPoint.from(p3));
        tm.accept(NopPoint.from(p4));
        tm.accept(NopPoint.from(p5));
        tm.accept(NopPoint.from(p6));

        tm.flushAllTracks();

        assertThat("Two tracks are emitted", sink.size(), is(2));
        assertThat("The first has exactly 1 point", sink.get(0).size(), is(1));
        assertThat("The second has exactly 5 points", sink.get(1).size(), is(5));
    }
}
