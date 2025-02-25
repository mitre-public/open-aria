
package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.core.TestUtils.confirmNopEquality;
import static org.mitre.openaria.core.Tracks.createTrackFromFile;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseNopTime;

import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;

import org.mitre.caasd.commons.TimeWindow;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Test;


public class TrackTest {


    @Test
    public void testKNearestPoints() {

        Track<NopHit> t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        Instant time = parseNopTime("07/08/2017", "14:11:59.454");

        NavigableSet<Point<NopHit>> knn = t1.kNearestPoints(time, 5);

        confirmNopEquality(
            knn,
            "[RH],STARS,GEG,07/08/2017,14:11:50.254,,,,1200,38,132,264,47.5884,-117.65535,655,0,-4.246,-1.8789,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}",
            "[RH],STARS,GEG,07/08/2017,14:11:55.044,,,,1200,39,134,250,47.58755,-117.65948,655,0,-4.4141,-1.9297,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}",
            "[RH],STARS,GEG,07/08/2017,14:11:59.454,,,,1200,39,132,257,47.58689,-117.66352,655,0,-4.5782,-1.9688,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}",
            "[RH],STARS,GEG,07/08/2017,14:12:04.134,,,,1200,39,133,246,47.58585,-117.66736,655,0,-4.7343,-2.0313,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}",
            "[RH],STARS,GEG,07/08/2017,14:12:08.734,,,,1200,39,133,248,47.58448,-117.67102,655,0,-4.8828,-2.1132,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}"
        );
    }

    @Test
    public void testInterpolate_bug117() {
        /*
         * Bug 117 is when creating an interpolated point at the very beginning of a track is
         * failing. This unit test is designed to FAIL due to this bug. When this unit test passes
         * it is a sign that the bug has been fixed.
         */

        Point<NopHit> p1 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,150,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p2 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,150,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p3 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p4 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p5 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,151,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p6 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,151,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

        Instant t1 = parseNopTime("06/30/2017", "16:40:17.000");

        Track<NopHit> fullTrack = Track.of(newArrayList(p1, p2, p3, p4, p5, p6));

        Optional<Point<NopHit>> result = fullTrack.interpolatedPoint(t1);
        assertTrue(result.isPresent());
        assertTrue(result.get().time().equals(t1));
    }

    @Test
    public void testGetOverlapWith() {

        Point<NopHit> p1 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,150,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p2 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,150,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p3 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p4 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p5 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,151,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p6 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,151,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

        Instant t1 = parseNopTime("06/30/2017", "16:40:17.000");
        Instant t2 = parseNopTime("06/30/2017", "16:40:29.000");
        Instant t3 = parseNopTime("06/30/2017", "16:40:42.000");

        Track<NopHit> fullTrack = Track.of(newArrayList(p1, p2, p3, p4, p5, p6));
        Track<NopHit> firstHalf = Track.of(newArrayList(p1, p2, p3));
        Track<NopHit> secondHalf = Track.of(newArrayList(p4, p5, p6));

        Track<NopHit> firstAndThirdPoints = Track.of(newArrayList(p1, p3));
        Track<NopHit> secondAndFourthPoints = Track.of(newArrayList(p2, p4));

        assertEquals(
            fullTrack.getOverlapWith(firstHalf).get(),
            TimeWindow.of(t1, t3),
            "This is a regular overlap"
        );
        assertEquals(
            firstHalf.getOverlapWith(fullTrack).get(),
            TimeWindow.of(t1, t3),
            "Changing the order of the auguments of the above assertion doesn't matter"
        );

        assertEquals(
            firstAndThirdPoints.getOverlapWith(secondAndFourthPoints).get(),
            TimeWindow.of(t2, t3),
            "This is a regular overlap (but when all the points have different times)"
        );

        assertFalse(
            firstHalf.getOverlapWith(secondHalf).isPresent(),
            "There is no overlap in this case"
        );
        assertFalse(
            secondHalf.getOverlapWith(firstHalf).isPresent(),
            "Same as above, but with arguments in a different order"
        );
    }

    @Test
    public void subset_predicate() {

        Track<NopHit> t1 = createTrackFromFile(new File("src/test/resources/Track1.txt"));

        int NUM_TRACK_POINTS = 63;

        assertThat(t1.size(), is(NUM_TRACK_POINTS));

        //get everything
        Collection<Point<NopHit>> everything = t1.subset(p -> true);
        assertThat(everything, hasSize(NUM_TRACK_POINTS));

        //get nothing
        Collection<Point<NopHit>> nothing = t1.subset(p -> false);
        assertThat(nothing, hasSize(0));

        Collection<Point<NopHit>> lowSpeedPoints = t1.subset(pt -> nonNull(pt.speed()) && pt.speed().inKnots() < 90);

        //get something...
        assertThat(lowSpeedPoints, not(empty()));
        assertThat(lowSpeedPoints.size(), lessThan(NUM_TRACK_POINTS));
        //and ensure the condition is always matched.
        for (Point lowSpeedPoint : lowSpeedPoints) {
            assertTrue(lowSpeedPoint.speed().inKnots() < 90);
        }
    }

    @Test
    public void subset_reflectsEndTime() {

        Track<NopHit> t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        //this is the time of 21st point in the track
        Instant endTime = parseNopTime("07/08/2017", "14:10:45.534");

        NavigableSet<Point<NopHit>> subset = t1.subset(Instant.EPOCH, endTime);

        assertThat(subset, hasSize(21));
        assertThat(subset.last().time(), is(endTime));
    }

    @Test
    public void subset_reflectsStartTime() {

        Track<NopHit> t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        //this is the time of 21st point in the track
        Instant startTime = parseNopTime("07/08/2017", "14:10:45.534");

        NavigableSet<Point<NopHit>> subset = t1.subset(startTime, startTime.plus(365 * 20, DAYS));

        assertThat(subset, hasSize(t1.size() - 21 + 1)); //"+1" because the fence post Point is in both the original track and the subset

        //the first point in the subset has the correct time
        assertThat(subset.first().time(), is(startTime));
    }

    @Test
    public void subset_reflectsStartAndEndTimes() {

        Track<NopHit> t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        Instant startTime = parseNopTime("07/08/2017", "14:10:45.534");
        Instant endTime = parseNopTime("07/08/2017", "14:11:17.854");

        NavigableSet<Point<NopHit>> subset = t1.subset(startTime, endTime);

        assertThat(subset, hasSize(8));

        assertThat(subset.first().time(), is(startTime));
        assertThat(subset.last().time(), is(endTime));
    }
}
