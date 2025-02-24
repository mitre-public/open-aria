package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.SingleAircraftRecord.mean;
import static org.mitre.openaria.airborne.SingleAircraftRecord.parseJson;
import static org.mitre.openaria.smoothing.TrackSmoothing.coreSmoothing;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Test;

public class SingleAircraftRecordTest {

    static final TrackPair<NopHit> TEST_TRACKS = makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));

    static final Track<NopHit> TRACK_1 = smooth(TEST_TRACKS.track1());
    static final Track<NopHit> TRACK_2 = smooth(TEST_TRACKS.track2());

    //smooth the test tracks to correct missing values...
    private static Track<NopHit> smooth(Track<NopHit> rawTrack) {
        DataCleaner<Track<NopHit>> cleaner = coreSmoothing();
        return cleaner.clean(rawTrack).orElseThrow();
    }

    @Test
    public void canBuildRecord() {

        assertDoesNotThrow(
            () -> new SingleAircraftRecord(
                TRACK_1,
                TRACK_1.asTimeWindow().instantWithin(.5),
                "thisIsAFunTrackHash"
            )
        );
    }

    @Test
    public void toJson_fromJson_cycleIsConsistent() {
        SingleAircraftRecord exampleRecord = new SingleAircraftRecord(
            TRACK_1,
            TRACK_1.asTimeWindow().instantWithin(.5),
            "thisIsAFunTrackHash"
        );

        String json = exampleRecord.asJson();

//		System.out.println(json);

        SingleAircraftRecord record_round2 = parseJson(json);

        String json2 = record_round2.asJson();

        assertThat(json, is(json2));
    }

    @Test
    public void canCreateRecordAtBeginningOfTrack() {
        //this may fail if the climbrate computation is flawed at the beginning of the track
        SingleAircraftRecord exampleRecord = new SingleAircraftRecord(
            TRACK_1,
            TRACK_1.asTimeWindow().instantWithin(0),
            "thisIsAFunTrackHash"
        );
    }

    @Test
    public void canCreateRecordAtEndOfTrack() {
        //this may fail if the climbrate computation is flawed at the end of the track
        SingleAircraftRecord exampleRecord = new SingleAircraftRecord(
            TRACK_1,
            TRACK_1.asTimeWindow().instantWithin(1),
            "thisIsAFunTrackHash"
        );
    }

    @Test
    public void meanSpeedIsComputedCorrectly() {
        Speed negativeTenKnots = Speed.of(-10, KNOTS);
        Speed oneKnot = Speed.of(1, KNOTS);
        Speed fiveKnots = Speed.of(5, KNOTS);
        Speed nineKnots = Speed.of(9, KNOTS);

        assertThat(
            mean(oneKnot, nineKnots, fiveKnots).toString(5), //avg of 1, 5, 9
            is(Speed.of(5.0, KNOTS).toString(5))
        );

        assertThat(
            mean(oneKnot, nineKnots, negativeTenKnots).toString(5), //avg of 1, 9, -10
            is(Speed.of(0.0, KNOTS).toString(5))
        );
    }
}
