
package org.mitre.openaria.smoothing;

import static org.apache.commons.math3.util.FastMath.sqrt;

import java.time.Instant;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class LateralOutlierDetector implements DataCleaner<Track> {

    private final int REQUIRED_SAMPLE_SIZE = 9;

    /*
     * A Point's actual LatLong and predicted LatLong must differ by at least this much before the
     * Point's location can be deemed an outlier.
     */
    private final Distance MIN_QUALIFYING_ERROR = Distance.ofNauticalMiles(0.05);

    /**
     * Find the Points in the input track with outlying latitude-longitude locations;
     *
     * @param track A Track
     *
     * @return The set of Point in this Track with outlying latitude-longitude locations.
     */
    public NavigableSet<Point> getOutliers(Track track) {

        TreeSet<Point> outliers = new TreeSet<>();

        // the for loop is wonky due to the raw type, probably could be improved
        for (Point point : ((NavigableSet<Point<?>>) track.points())) {

            LateralAnalysisResult result = analyzePoint(point, track);

            if (result.isOutlier) {
                outliers.add(point);
            }
        }

        return outliers;
    }

    /**
     * This method analyzes the LatLong location of a single Point within a Track and determines if
     * that Point has an outlying LatLong.
     * <p>
     * The determination is made by comparing Linear Regressions: Two regression using time and
     * LatLong data from ONLY neighboring Point. And two other regression that also include the
     * testPoint's (time, LatLong) data-point.
     * <p>
     * A Point is declared an outlier if adding the TestPoint's data causes a large drop in the
     * rSquared of the regression AND the Mean Squared Error of the full regression is "relevant".
     *
     * @param testPoint A single point within the track
     * @param track     The source track
     *
     * @return A LateralAnalysisResult object that describes whether or not the testPoint is an
     *     outlier.
     */
    private LateralAnalysisResult analyzePoint(Point testPoint, Track track) {

        Collection<Point> pointsNearby = track.kNearestPoints(
            testPoint.time(),
            REQUIRED_SAMPLE_SIZE
        );

        if (pointsNearby.size() < REQUIRED_SAMPLE_SIZE) {
            //When the sample size is small do not declare outliers and do not provide predictions
            return new LateralAnalysisResult(false);
        }

        LateralRegression localRegression = new LateralRegression(pointsNearby, testPoint);

        LatLong predictedLocation = localRegression.predictLocation(testPoint.time());
        Distance locationError = predictedLocation.distanceTo(testPoint.latLong());

        //BEFORE incorporating the test point
        //compute the (geometric) mean of the rSquared value from the latitude and longitude regressions
        double latRSquareWithout = rSquaredOf(localRegression.latRegression);
        double longRSquareWithout = rSquaredOf(localRegression.longRegression);
        double combinedRSquareWithout = sqrt(latRSquareWithout * longRSquareWithout);

        localRegression.incorporatePoint(testPoint);

        //AFTER incorporating the test point
        //compute the (geometric) mean of the rSquared value from the latitude and longitude regressions
        double latRSquareWith = rSquaredOf(localRegression.latRegression);
        double longRSquareWith = rSquaredOf(localRegression.longRegression);
        double combinedRSquareWith = sqrt(latRSquareWith * longRSquareWith);

        //We "detect" an outlier if adding the test point dramatically degrades the test fit
        boolean largeDropInR = combinedRSquareWithout - combinedRSquareWith > 0.3;

        boolean isOutlier = largeDropInR && locationError.isGreaterThan(MIN_QUALIFYING_ERROR);

        return new LateralAnalysisResult(isOutlier);
    }

    private static double rSquaredOf(SimpleRegression regression) {
        //catch the case when the regression has zero error and the rSquare is NaN
        return (Double.isNaN(regression.getRSquare())
            ? 1.0
            : regression.getRSquare());
    }

    /**
     * Create a cleaned version of the input track that removes any Points that had an outlier
     * LatLong location.
     *
     * @param inputTrack A Track
     *
     * @return An Optional Track with lateral outliers removed.
     */
    @Override
    public Optional<Track> clean(Track inputTrack) {

        Collection<Point> outliers = getOutliers(inputTrack);

        TreeSet<Point> points = new TreeSet<>(inputTrack.points());
        points.removeAll(outliers);

        return points.isEmpty()
            ? Optional.empty()
            : Optional.of(Track.ofRaw(points));
    }

    private static class LateralAnalysisResult {

        final boolean isOutlier;

        LateralAnalysisResult(boolean isOutlier) {
            this.isOutlier = isOutlier;
        }
    }

    /*
     * A LateralRegression puts LatLong locations from a collection of Points into two Regressions
     * (one for latitude, one for longitude).
     */
    private static class LateralRegression {

        SimpleRegression latRegression;
        SimpleRegression longRegression;

        LateralRegression(Collection<Point> points, Point testPoint) {
            this.latRegression = new SimpleRegression();
            this.longRegression = new SimpleRegression();

            for (Point point : points) {
                if (point == testPoint) {
                    continue;
                }
                incorporatePoint(point);
            }
        }

        private void incorporatePoint(Point p) {
            long time = p.time().toEpochMilli();
            latRegression.addData(time, p.latLong().latitude());
            longRegression.addData(time, p.latLong().longitude());
        }

        /**
         * @param time The time at which the location prediction is made.
         *
         * @return A predicted LatLong location given the Point currently incorporated in this
         *     regression pair.
         */
        private LatLong predictLocation(Instant time) {
            return LatLong.of(
                latRegression.predict(time.toEpochMilli()),
                longRegression.predict(time.toEpochMilli())
            );
        }
    }
}
