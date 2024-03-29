
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.Tracks.computeTimeInCloseProximity;
import static org.mitre.openaria.core.Tracks.createTrackFromFile;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.caasd.commons.parsing.nop.NopParsingUtils.parseNopTime;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.parsing.nop.Facility;


public class TracksTest {

    @Test
    public void testAsNop_rawNop() {

        String raw1 = "[RH],Center,ZOB_B,07-10-2016,17:24:37.000,RPA4391,E170,L,7336,270,443,077,41.3725,-80.8414,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763689,IND,1813,270//270,,L,1,,,{RH}";
        String raw2 = "[RH],Center,ZOB_B,07-10-2016,17:24:49.000,RPA4391,E170,L,7336,270,444,077,41.3781,-80.8100,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763984,IND,1813,270//270,,L,1,,,{RH}";
        String raw3 = "[RH],Center,ZOB_B,07-10-2016,17:25:02.000,RPA4391,E170,L,7336,270,444,077,41.3839,-80.7778,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616764278,IND,1813,270//270,,L,1,,,{RH}";

        Point p1 = NopPoint.from(raw1);
        Point p2 = NopPoint.from(raw2);
        Point p3 = NopPoint.from(raw3);
        List<Point> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);

        Track track = new SimpleTrack(points);

        assertEquals(
            raw1 + "\n" + raw2 + "\n" + raw3 + "\n",
            track.asNop()
        );
    }

    @Test
    public void testAsNop_customPoints() {

        Point p1 = (new PointBuilder())
            .time(Instant.EPOCH)
            .facility("A80")
            .latLong(0.0, 1.0)
            .build();

        Point p2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(4))
            .callsign("AA123")
            .latLong(0.0, 1.0)
            .build();

        Point p3 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(8))
            .latLong(0.0, 1.0)
            .build();

        List<Point> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);

        Track track = new SimpleTrack(points);

        assertEquals(
            "[RH],STARS,A80,01/01/1970,00:00:00.000,,,,null,,,,0.00000,1.00000,null,,,,,,,null,,,,,,,,,,,,,,,,,,,{RH}\n"
                + "[RH],STARS,null,01/01/1970,00:00:04.000,AA123,,,null,,,,0.00000,1.00000,null,,,,,,,null,,,,,,,,,,,,,,,,,,,{RH}\n"
                + "[RH],STARS,null,01/01/1970,00:00:08.000,,,,null,,,,0.00000,1.00000,null,,,,,,,null,,,,,,,,,,,,,,,,,,,{RH}\n",
            track.asNop()
        );
    }

    @Test
    public void testTrackDistance() {

        Track t1 = createTrackFromFile(getResourceFile("Track1.txt"));
        Track t2 = createTrackFromFile(getResourceFile("Track2.txt"));

        double dist = Tracks.maxDistBetween(t1, t2);

        System.out.println("Max distance = " + dist);

        //@todo this is a crappy test.  It doesn't verify the distance.
    }

    @Test
    public void testInterpolatedPoint_outsideTimeWindow() {

        Track t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        Optional<Point> p = t1.interpolatedPoint(Instant.EPOCH);

        assertFalse(
            p.isPresent(),
            "This should be empty because the time is outside the TimeWindow of the sample track"
        );
    }

    @Test
    public void testInterpolatedPoint_insdieTimeWindow() {

        Track t1 = createTrackFromFile(getResourceFile("Track1.txt"));

        //pick an arbitrary time "within" this track
        Instant time = parseNopTime("07/08/2017", "14:11:03.999");

        Optional<Point> p = t1.interpolatedPoint(time);

        /*
         * The interpolated point occurs between these two radar hits..
         *
         * [RH],STARS,GEG,07/08/2017,14:11:03.954,,,,1200,36,130,258,47.59298,-117.61476,655,0,-2.5976,-1.6055,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}
         * [RH],STARS,GEG,07/08/2017,14:11:08.584,,,,1200,36,131,264,47.59259,-117.6188,655,0,-2.7617,-1.6289,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}
         */
        assertTrue(p.isPresent(), "The new point should exist");
        assertTrue(p.get().time().equals(time), "The interpolated point's time should be as selected");

        Instant startTime = parseNopTime("07/08/2017", "14:11:03.954");
        Instant endTime = parseNopTime("07/08/2017", "14:11:08.584");

        TimeWindow window = TimeWindow.of(startTime, endTime);
        double fraction = window.toFractionOfRange(time); //

        double latitude = Interpolate.interpolate(47.59298, 47.59259, fraction);
        double longitude = Interpolate.interpolate(-117.61476, -117.6188, fraction);

        assertEquals(p.get().latLong(), LatLong.of(latitude, longitude));
        assertTrue(Facility.toFacility(p.get().facility()) == Facility.GEG);
    }

    @Test
    public void testComputeTimeInCloseProximity() {

        Track t1 = createTrackFromResource(Tracks.class, "twoMilitaryAircraft_part1.txt");
        Track t2 = createTrackFromResource(Tracks.class, "twoMilitaryAircraft_part2.txt");

        Duration durationWithinOneHalfNM = computeTimeInCloseProximity(
            t1,
            t2,
            Duration.ofSeconds(1),
            0.5
        );

        assertTrue(durationWithinOneHalfNM.getSeconds() < 80);
        assertTrue(60 < durationWithinOneHalfNM.getSeconds());

        Duration durationWithinOneMile = computeTimeInCloseProximity(
            t1,
            t2,
            Duration.ofSeconds(1),
            1.0
        );

        assertTrue(durationWithinOneMile.getSeconds() < 180);
        assertTrue(150 < durationWithinOneMile.getSeconds());
    }
}
