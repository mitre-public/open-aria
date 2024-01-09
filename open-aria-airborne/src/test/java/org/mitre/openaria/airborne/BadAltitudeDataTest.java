
package org.mitre.openaria.airborne;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;

/**
 * The altitude field in NOP data can get confused when two aircraft fly directly over top of one
 * another. When flying over each other the tracks will have extremely similar lat/long locations
 * but wildly different (true) altitudes. The NOP system can incorrectly set altitude values to zero
 * due to the confusion between the two flights. When an altitude value changes incorrectly the Risk
 * Metric computation can think aircraft that were previously separated by a large vertical distance
 * are headed towards each other at high (vertical) speeds.
 * <p>
 * This test confirms that the defaultCleaner correctly removes some flaws in raw altitude data.
 */
public class BadAltitudeDataTest {

    @Test
    public void testRiskMetricOnDataWithFlawedAltitudeValues() {

        AirborneAlgorithmDef props = new AirborneAlgorithmDef();

        //This is the Track cleaner that is used to
        DataCleaner<Track> cleaner = props.singleTrackCleaner();

        for (TrackPair pair : getTrackPairsContainingAFlawedAltitudePoint()) {
            confirmAtLeastOneTrackContainsJumpyAltitudeData(pair);
            confirmTrackContainSmoothAltitudeData(cleaner.clean(pair.track1()).get());
            confirmTrackContainSmoothAltitudeData(cleaner.clean(pair.track2()).get());
        }
    }

    private void confirmAtLeastOneTrackContainsJumpyAltitudeData(TrackPair testCase) {

        Distance QUALIFYING_ALTITUDE_JUMP = Distance.ofFeet(400);

        assertTrue(
            containsAltitudeJump(testCase.track1(), QUALIFYING_ALTITUDE_JUMP)
                || containsAltitudeJump(testCase.track2(), QUALIFYING_ALTITUDE_JUMP),
            "At least one altitude jump should be found"
        );
    }

    private void confirmTrackContainSmoothAltitudeData(Track track) {

        Distance QUALIFYING_JUMP = Distance.ofFeet(400);

        assertFalse(containsAltitudeJump(track, QUALIFYING_JUMP));
    }

    private boolean containsAltitudeJump(Track track, Distance qualifingJump) {

        Point lastPoint = null;

        for (Point point : track.points()) {
            if (lastPoint == null) {
                lastPoint = point;
                continue;
            }

            Distance altitudeDelta = (point.altitude().minus(lastPoint.altitude())).abs();

            if (altitudeDelta.isGreaterThan(qualifingJump)) {
                return true;
            }

            lastPoint = point;
        }

        return false;
    }

    /**
     * Each of these TrackPairs contains one Track with a dropped (aka 0) altitude value.
     *
     * @return A couple test cases
     */
    private TrackPair[] getTrackPairsContainingAFlawedAltitudePoint() {

        return new TrackPair[]{
            //Without altitude corrections this track has Flawed 1A event scores of 16 and 17
            //The next highest 1A event score is 80
            //			extractTestTracks("modeCSwap_1.txt"),
            //Without altitude corrections this track has Flawed 1A event scores of 0.8 and 0.8
            //the next highest 1A event score is 218
            makeTrackPairFromNopData(getResourceFile(("modeCSwap_2.txt")))
        };
    }
}
