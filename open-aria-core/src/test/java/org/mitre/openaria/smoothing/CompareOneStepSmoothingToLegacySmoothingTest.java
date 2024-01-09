//package org.mitre.aria.smoothing;
//
//import static java.lang.Math.abs;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.closeTo;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.mitre.aria.core.Tracks.createTrackFromResource;
//import static org.mitre.caasd.commons.CollectionUtils.zip;
//import static org.mitre.caasd.commons.util.NeighborIterator.newNeighborIterator;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Iterator;
//
//import org.junit.jupiter.api.Test;
//import org.mitre.aria.core.Point;
//import org.mitre.aria.core.Track;
//import org.mitre.caasd.commons.DataCleaner;
//import org.mitre.caasd.commons.Distance;
//import org.mitre.caasd.commons.Pair;
//import org.mitre.caasd.commons.Speed;
//import org.mitre.caasd.commons.util.IterPair;
//import org.mitre.caasd.commons.util.NeighborIterator;
//
//import com.google.common.math.StatsAccumulator;
//
//public class CompareOneStepSmoothingToLegacySmoothingTest {
//
//    //Use the old CrossTrackFilter and AlongTrackFilter (that are being deprecated)
//    private final DataCleaner<Track> legacySmoothing = MutableSmoother.of(
//        new TimeDownSampler(Duration.ofMillis(2_000)),
//        new CrossTrackFilter(),
//        new AlongTrackFilter()
//    );
//
//    //Use the new "one step" TrackFilter
//    private final DataCleaner<Track> oneStepSmoothing = MutableSmoother.of(
//        new TimeDownSampler(Duration.ofMillis(2_000)),
//        new TrackFilter()
//    );
//
//    @Test
//    public void compareLegacySmoothingToNewOneStepSmoothing() {
//
//        Track rawTrackWithNoCurvatureInfo = createTrackFromResource(
//            LateralOutlierDetector.class,
//            "trackWithSomeGentalError.txt"
//        );
//
//        Track legacyResult = legacySmoothing.clean(rawTrackWithNoCurvatureInfo.mutableCopy()).get();
//        Track oneStepResult = oneStepSmoothing.clean(rawTrackWithNoCurvatureInfo.mutableCopy()).get();
//
//        assertThat(
//            "Both smoothers produce the same number of points",
//            legacyResult.size(), is(oneStepResult.size())
//        );
//
//        //All points have the same time values
//        verifyPointTimesAreTheSame(legacyResult, oneStepResult);
//
//        //The along track distances are indeed set (no value judgement, just non-null stuff)
//        verifyAlongTrackDistancesAreSet(legacyResult, oneStepResult);
//        verifyCoursesAreSet(legacyResult);
//        verifyCoursesAreSet(oneStepResult);
//        verifySpeedsAreSet(legacyResult);
//        verifySpeedsAreSet(oneStepResult);
//        verifyAlongTrackDistance(legacyResult, 0.01);
//        verifyAlongTrackDistance(oneStepResult, 0.0);
//
//        compareInternalSpeedAndDistancesConsistency(legacyResult, oneStepResult);
//
//        verifySpeedConsistency(legacyResult, oneStepResult);
//        verifyCurvatureConsistency(legacyResult, oneStepResult);
//
//        printLatLongDataForGraphics(rawTrackWithNoCurvatureInfo, legacyResult, oneStepResult);
//    }
//
//    private void verifyPointTimesAreTheSame(Track legacyResult, Track oneStepResult) {
//
//        Iterator<? extends Pair<? extends Point, ? extends Point>> comboIterator =
//            zip(legacyResult.points(),oneStepResult.points());
//
//        while(comboIterator.hasNext()) {
//            Pair<Point, Point> pair = (Pair<Point, Point>) comboIterator.next();
//            Point legacy = pair.first();
//            Point oneStep = pair.second();
//
//            assertThat(legacy.time(), is(oneStep.time()));
//        }
//    }
//
//    private void verifyAlongTrackDistancesAreSet(Track legacyResult, Track oneStepResult) {
//        legacyResult.points().forEach(
//            pt -> assertThat(pt.alongTrackDistance(), notNullValue())
//        );
//        oneStepResult.points().forEach(
//            pt -> assertThat(pt.alongTrackDistance(), notNullValue())
//        );
//    }
//
//    private void verifyCoursesAreSet(Track track) {
//        track.points().forEach(
//            pt -> assertThat(pt.course(), notNullValue())
//        );
//    }
//
//    private void verifySpeedsAreSet(Track track) {
//        track.points().forEach(
//            pt -> assertThat(pt.speedInKnots(), notNullValue())
//        );
//    }
//
//    private void verifyAlongTrackDistance(Track result, double tolerance) {
//
//        StatsAccumulator stats = new StatsAccumulator(); //for tracking aggregate statistics
//
//        NeighborIterator<Point> pairIterator = newNeighborIterator(result.points());
//
//        Distance totalPtToPtDistance = Distance.ZERO;
//
//        while(pairIterator.hasNext()) {
//            IterPair<Point> pair = pairIterator.next();
//
//            totalPtToPtDistance = totalPtToPtDistance.plus(Distance.between(pair.prior().latLong(), pair.current().latLong()));
//
//            //measure difference between computed cumulative distance and the points alongTrackDistance
//            double deltaInNm = totalPtToPtDistance.inNauticalMiles() - pair.current().alongTrackDistance();
//            double relativeError = abs(deltaInNm / totalPtToPtDistance.inNauticalMiles());
//
//            stats.add(relativeError);
//        }
//
//        assertThat(stats.mean(), closeTo(0.0, tolerance));
//    }
//
//    private void compareInternalSpeedAndDistancesConsistency(Track legacyResult, Track oneStepResult) {
//
//        StatsAccumulator stats_legacy = new StatsAccumulator(); //for tracking aggregate statistics
//        StatsAccumulator stats_oneStep = new StatsAccumulator(); //for tracking aggregate statistics
//
//
//        NeighborIterator<Point> legacyIterator = (NeighborIterator<Point>) newNeighborIterator(legacyResult.points());
//        NeighborIterator<Point> oneStepIterator = (NeighborIterator<Point>) newNeighborIterator(oneStepResult.points().iterator());
//
//        while (legacyIterator.hasNext()) {
//
//            IterPair<Point> legacyPair = legacyIterator.next();
//            IterPair<Point> oneStepPair = oneStepIterator.next();
//
//            Point priorLegacy = legacyPair.prior();
//            Point priorOneStep = oneStepPair.prior();
//            Point curLegacy = legacyPair.current();
//            Point curOneStep = oneStepPair.current();
//
//            Distance distBtwPoints_legacy = priorLegacy.latLong().distanceTo(curLegacy.latLong());
//            Distance distBtwPoints_oneStep = priorOneStep.latLong().distanceTo(curOneStep.latLong());
//
//            Speed speed_legacy = Speed.ofKnots(priorLegacy.speedInKnots());
//            Speed speed_oneStep = Speed.ofKnots(priorOneStep.speedInKnots());
//
//            Duration legacyTimeDelta = Duration.between(priorLegacy.time(), curLegacy.time());
//            Duration oneStepTimeDelta = Duration.between(priorOneStep.time(), curOneStep.time());
//
//            //Distance you SHOULD move given the starting speed and the "time until next point"
//            Distance expectedDist_legacy = speed_legacy.times(legacyTimeDelta);
//            Distance expectedDist_oneStep = speed_oneStep.times(oneStepTimeDelta);
//
//            //Measure how internally consistent the speed and location data is
//            //These values should be approximately 1.0 plus/minus the impact of acceleration (which should be small over a short time span)
//            double consistency_legacy = distBtwPoints_legacy.dividedBy(expectedDist_legacy);
//            double consistency_oneStep = distBtwPoints_oneStep.dividedBy(expectedDist_oneStep);
//
//            stats_legacy.add(consistency_legacy);
//            stats_oneStep.add(consistency_oneStep);
//
////            System.out.println(consistency_legacy + "\t" + consistency_oneStep);
//        }
//
////        System.out.println("Legacy");
////        System.out.println("  Min : " + stats_legacy.min());
////        System.out.println("  Mean: " + stats_legacy.mean());
////        System.out.println("  Max : " + stats_legacy.max());
////        System.out.println("  Min-to-Max (range): " + (stats_legacy.max() - stats_legacy.min()));
////        System.out.println("  StandardDev : " + stats_legacy.populationStandardDeviation());
////
////        System.out.println("OneStep");
////        System.out.println("  Min : " + stats_oneStep.min());
////        System.out.println("  Mean: " + stats_oneStep.mean());
////        System.out.println("  Max : " + stats_oneStep.max());
////        System.out.println("  Min-to-Max (range): " + (stats_oneStep.max() - stats_oneStep.min()));
////        System.out.println("  StandardDev : " + stats_oneStep.populationStandardDeviation());
//
//        //Both means are close to 1.0
//        assertThat(stats_legacy.mean(), closeTo(1.0, 0.02));
//        assertThat(stats_oneStep.mean(), closeTo(1.0, 0.02));
//
//        //BUT!  The legacy smoothing has concerning outliers
//        assertThat(stats_legacy.max() , closeTo(3.466, 0.001)); //almost 3.5x the expected 1.0!
//        assertThat(stats_legacy.min() , closeTo(0.254, 0.001)); //only 25% of the expected value
//        assertThat(stats_legacy.populationStandardDeviation(), closeTo(0.209, 0.001)); //when mean ~= 1.0 a standard Dev of .2 is ALOT
//
//        //FORTUNATELY, The newer smoothing is better
//        assertThat(stats_oneStep.max() , closeTo(1.196, 0.001));
//        assertThat(stats_oneStep.min() , closeTo(0.805, 0.001));
//        assertThat(stats_oneStep.populationStandardDeviation(), closeTo(0.0308, 0.001)); //when mean ~= 1.0 a standard Dev of .03 is
//    }
//
//    private void verifySpeedConsistency(Track legacyResult, Track oneStepResult) {
//
//        Iterator<Point> legacyIterator = (Iterator<Point>) legacyResult.points().iterator();
//        Iterator<Point> oneStepIterator = (Iterator<Point>) oneStepResult.points().iterator();
//
//        StatsAccumulator stats = new StatsAccumulator(); //for tracking aggregate statistics
//
//
//        while(legacyIterator.hasNext() && oneStepIterator.hasNext()) {
//            Point legacy = legacyIterator.next();
//            Point oneStep = oneStepIterator.next();
//
//            double speedDelta = abs(legacy.speedInKnots() - oneStep.speedInKnots());
//
//            stats.add(speedDelta);
//        }
//
////        System.out.println("Speed Deltas");
////        System.out.println("  Min : " + stats.min());
////        System.out.println("  Mean: " + stats.mean());
////        System.out.println("  Max : " + stats.max());
////        System.out.println("  Min-to-Max (range): " + (stats.max() - stats.min()));
////        System.out.println("  StandardDev : " + stats.populationStandardDeviation());
//
//        //BUT!  The Speed estimates provided by both methods are similar
//        assertThat(stats.max() , closeTo(20.661, 0.001)); //The biggest delta is 20knots
//        assertThat(stats.mean() , closeTo(1.386, 0.001)); //The average delta is 1.4 knots
//        assertThat(stats.min() , closeTo(0.000, 0.001)); //only 25% of the expected value
//        assertThat(stats.populationStandardDeviation(), closeTo(2.817, 0.001)); //The std dev is only 2.8 knots
//    }
//
//    private void verifyCurvatureConsistency(Track legacyResult, Track oneStepResult) {
//
//        StatsAccumulator stats = new StatsAccumulator(); //for tracking aggregate statistics
//
//        NeighborIterator<Point> legacyIterator = (NeighborIterator<Point>) newNeighborIterator(legacyResult.points());
//        NeighborIterator<Point> oneStepIterator = (NeighborIterator<Point>) newNeighborIterator(oneStepResult.points().iterator());
//
//
//        while(legacyIterator.hasNext() && oneStepIterator.hasNext()) {
//
//            IterPair<Point> legacyPair = legacyIterator.next();
//            IterPair<Point> oneStepPair = oneStepIterator.next();
//
//            Point priorLegacy = legacyPair.prior();
//            Point priorOneStep = oneStepPair.prior();
//            Point curLegacy = legacyPair.current();
//            Point curOneStep = oneStepPair.current();
//
////            System.out.println((curLegacy.course() - priorLegacy.course()) + "\t" + (curOneStep.course() - priorOneStep.course()));
////            System.out.println("  " + curLegacy.curvature()+ "\t" + curOneStep.curvature());
//
////            System.out.println(priorLegacy.curvature() + "\t" + priorOneStep.curvature());
//
////            double speedDelta = abs(legacy.speedInKnots() - oneStep.speedInKnots());
////
////            stats.add(speedDelta);
//        }
//
////        System.out.println("Speed Deltas");
////        System.out.println("  Min : " + stats.min());
////        System.out.println("  Mean: " + stats.mean());
////        System.out.println("  Max : " + stats.max());
////        System.out.println("  Min-to-Max (range): " + (stats.max() - stats.min()));
////        System.out.println("  StandardDev : " + stats.populationStandardDeviation());
////
////        //BUT!  The Speed estimates provided by both methods are similar
////        assertThat(stats.max() , closeTo(20.661, 0.001)); //The biggest delta is 20knots
////        assertThat(stats.mean() , closeTo(1.386, 0.001)); //The average delta is 1.4 knots
////        assertThat(stats.min() , closeTo(0.000, 0.001)); //only 25% of the expected value
////        assertThat(stats.populationStandardDeviation(), closeTo(2.817, 0.001)); //The std dev is only 2.8 knots
//    }
//
//    private void printLatLongDataForGraphics(Track inputTrack, Track legacyResult, Track oneStepResult) {
//
//        Iterator<Point> inputIterator = (Iterator<Point>) inputTrack.points().iterator();
//        Iterator<Point> legacyIterator = (Iterator<Point>) legacyResult.points().iterator();
//        Iterator<Point> oneStepIterator = (Iterator<Point>) oneStepResult.points().iterator();
//
//        Instant anchorTime = inputTrack.points().first().time();
//
//        while (inputIterator.hasNext() && legacyIterator.hasNext() && oneStepIterator.hasNext()) {
//            Point inputPt = inputIterator.next();
//            Point legacy = legacyIterator.next();
//            Point oneStep = oneStepIterator.next();
//
////            System.out.println(
////                secOffset(anchorTime, inputPt.time()) + "\t" + inputPt.latitude() + "\t" + inputPt.longitude()
////                    + "\t\t" +secOffset(anchorTime, legacy.time()) + "\t" + legacy.latitude() + "\t" + legacy.longitude()
////                    + "\t\t" +secOffset(anchorTime, oneStep.time()) + "\t" + oneStep.latitude() + "\t" + oneStep.longitude());
//
//        }
//    }
//
//    private double secOffset(Instant anchorTime, Instant time) {
//        return ((double) Duration.between(anchorTime, time).toMillis()) / 1000.0;
//    }
//}
