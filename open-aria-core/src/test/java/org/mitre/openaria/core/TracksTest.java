
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.Tracks.*;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseNopTime;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.openaria.core.formats.NopEncoder;
import org.mitre.openaria.core.formats.NopHit;
import org.mitre.openaria.core.temp.Extras.AircraftDetails;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;

import org.junit.jupiter.api.Test;


public class TracksTest {

    @Test
    public void testAsNop_rawNop() {

        String raw1 = "[RH],Center,ZOB_B,07-10-2016,17:24:37.000,RPA4391,E170,L,7336,270,443,077,41.3725,-80.8414,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763689,IND,1813,270//270,,L,1,,,{RH}";
        String raw2 = "[RH],Center,ZOB_B,07-10-2016,17:24:49.000,RPA4391,E170,L,7336,270,444,077,41.3781,-80.8100,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763984,IND,1813,270//270,,L,1,,,{RH}";
        String raw3 = "[RH],Center,ZOB_B,07-10-2016,17:25:02.000,RPA4391,E170,L,7336,270,444,077,41.3839,-80.7778,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616764278,IND,1813,270//270,,L,1,,,{RH}";

        Point<NopHit> p1 = NopHit.from(raw1);
        Point<NopHit> p2 = NopHit.from(raw2);
        Point<NopHit> p3 = NopHit.from(raw3);
        List<Point<NopHit>> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);

        Track<NopHit> track = Track.of(points);

        NopEncoder nopEncoder = new NopEncoder();

        assertEquals(
            raw1 + "\n" + raw2 + "\n" + raw3 + "\n",
            nopEncoder.asRawNop(track)
        );
    }

    @Test
    public void testAsNop_customPoints() {

        record PojoWithAcDetails(AircraftDetails acDetails) implements HasAircraftDetails {}

        Point<PojoWithAcDetails> p1 = Point.<PojoWithAcDetails>builder()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.0)
            .build();

        Point<PojoWithAcDetails> p2 = Point.<PojoWithAcDetails>builder()
            .time(Instant.EPOCH.plusSeconds(4))
            .rawData(new PojoWithAcDetails(new AircraftDetails("AA123", "BOEING")))
            .latLong(0.0, 1.0)
            .build();

        Point<PojoWithAcDetails> p3 = Point.<PojoWithAcDetails>builder()
            .time(Instant.EPOCH.plusSeconds(8))
            .latLong(0.0, 1.0)
            .build();

        List<Point<PojoWithAcDetails>> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);

        Track<PojoWithAcDetails> track = Track.of(points);

        NopEncoder nopEncoder = new NopEncoder();

        assertEquals(
            "[RH],STARS,,01/01/1970,00:00:00.000,,,,,,,,0.00000,1.00000,null,,,,,,,,,,,,,,,,,,,,,,,,,,{RH}\n"
                + "[RH],STARS,,01/01/1970,00:00:04.000,AA123,BOEING,,,,,,0.00000,1.00000,null,,,,,,,,,,,,,,,,,,,,,,,,,,{RH}\n"
                + "[RH],STARS,,01/01/1970,00:00:08.000,,,,,,,,0.00000,1.00000,null,,,,,,,,,,,,,,,,,,,,,,,,,,{RH}\n",
            nopEncoder.asRawNop(track)
        );
    }

    @Test
    public void testTrackDistance() {

        Track<NopHit> t1 = createTrackFromFile(new File("src/test/resources/Track1.txt"));
        Track<NopHit> t2 = createTrackFromFile(new File("src/test/resources/Track2.txt"));

        double dist = Tracks.maxDistBetween(t1, t2);

        System.out.println("Max distance = " + dist);

        //@todo this is a crappy test.  It doesn't verify the distance.
    }

    @Test
    public void testInterpolatedPoint_outsideTimeWindow() {

        Track<NopHit> t1 = createTrackFromFile(new File("src/test/resources/Track1.txt"));

        Optional<Point<NopHit>> p = t1.interpolatedPoint(Instant.EPOCH);

        assertFalse(
            p.isPresent(),
            "This should be empty because the time is outside the TimeWindow of the sample track"
        );
    }

    @Test
    public void testInterpolatedPoint_insdieTimeWindow() {

        Track<NopHit> t1 = createTrackFromFile(new File("src/test/resources/Track1.txt"));

        //pick an arbitrary time "within" this track
        Instant time = parseNopTime("07/08/2017", "14:11:03.999");

        Optional<Point<NopHit>> p = t1.interpolatedPoint(time);

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
    }

    @Test
    public void testComputeTimeInCloseProximity() {

        Track<NopHit> t1 = createTrackFromResource(Tracks.class, "twoMilitaryAircraft_part1.txt");
        Track<NopHit> t2 = createTrackFromResource(Tracks.class, "twoMilitaryAircraft_part2.txt");

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
