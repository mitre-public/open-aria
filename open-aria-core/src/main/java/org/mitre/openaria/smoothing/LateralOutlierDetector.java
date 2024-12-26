
package org.mitre.openaria.smoothing;

import static org.apache.commons.math3.util.FastMath.*;

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

public class LateralOutlierDetector<T> implements DataCleaner<Track<T>> {

    private final int REQUIRED_SAMPLE_SIZE = 9;

    /*
     * A Point's actual LatLong and predicted LatLong must differ by at least this much before the
     * Point's location can be deemed an outlier.
     */
    private final Distance MIN_QUALIFYING_ERROR = Distance.ofNauticalMiles(0.05);
//    private final Distance MIN_QUALIFYING_ERROR = Distance.ofNauticalMiles(0.025);

    /**
     * Find the Points in the input track with outlying latitude-longitude locations;
     *
     * @param track A Track
     *
     * @return The set of Point in this Track with outlying latitude-longitude locations.
     */
    public NavigableSet<Point<T>> getOutliers(Track<T> track) {

        TreeSet<Point<T>> outliers = new TreeSet<>();

        // the for loop is wonky due to the raw type, probably could be improved
        for (Point<T> point : track.points()) {

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
    private LateralAnalysisResult analyzePoint(Point<T> testPoint, Track<T> track) {

        Collection<Point<T>> pointsNearby = track.kNearestPoints(
            testPoint.time(),
            REQUIRED_SAMPLE_SIZE
        );

        if (pointsNearby.size() < REQUIRED_SAMPLE_SIZE) {
            //When the sample size is small do not declare outliers and do not provide predictions
            return new LateralAnalysisResult(false);
        }

        LateralRegression<T> localRegression = new LateralRegression<T>(pointsNearby, testPoint);

//        return oldMethod(localRegression, testPoint);
        return newMethod(localRegression, testPoint);
    }

    private LateralAnalysisResult newMethod(LateralRegression<T> localRegression, Point<T> testPoint) {

        double outlier_y_ness = localRegression.semiStudentizedResidual(testPoint.latLong(), testPoint.time());
        LatLong predictedLocation = localRegression.predictLocation(testPoint.time());
        Distance locationError = predictedLocation.distanceTo(testPoint.latLong());

        boolean isOutlier = outlier_y_ness > 15 && locationError.isGreaterThan(MIN_QUALIFYING_ERROR);

//        if (isOutlier) {
//            System.out.println("\nlocationError: " + locationError.inFeet());
//            System.out.println("  outlier_y_ness: " + outlier_y_ness);
//            System.out.println("  " + (new NopEncoder()).asRawNop(testPoint));
//            System.out.println("  predictedLocation: " + predictedLocation);
//            System.out.println("  isOutlier: " + isOutlier);
//        }

        return new LateralAnalysisResult(isOutlier);

    }

    private LateralAnalysisResult oldMethod(LateralRegression<T> localRegression, Point<T> testPoint) {

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

//        if (isOutlier) {
//            System.out.println("\nlocationError: " + locationError.inFeet());
//            System.out.println("  drop_in_R: " + (combinedRSquareWithout - combinedRSquareWith));
//            System.out.println("  " + (new NopEncoder()).asRawNop(testPoint));
//            System.out.println("  predictedLocation: " + predictedLocation);
//            System.out.println("  isOutlier: " + isOutlier);
//        }

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
    public Optional<Track<T>> clean(Track<T> inputTrack) {

        Collection<Point<T>> outliers = getOutliers(inputTrack);

        TreeSet<Point<T>> points = new TreeSet<>(inputTrack.points());
        points.removeAll(outliers);

        return points.isEmpty()
            ? Optional.empty()
            : Optional.of(Track.of(points));
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
    private static class LateralRegression<T> {

        SimpleRegression latRegression;
        SimpleRegression longRegression;

        LateralRegression(Collection<Point<T>> points, Point<T> testPoint) {
            this.latRegression = new SimpleRegression();
            this.longRegression = new SimpleRegression();

            for (Point<T> point : points) {
                if (point == testPoint) {
                    continue;
                }
                incorporatePoint(point);
            }
        }

        private void incorporatePoint(Point<T> p) {
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

        /**
         * Compute the "outlier-y-ness" of a LatLong location
         *
         * @param location A LatLong (that usually was NOT used to create these regressions)
         * @param time     The time that LatLong was observed
         *
         * @return The hypotenuse of the "Semi-Studentized Residual" from the Lat & Long fits
         */
        private double semiStudentizedResidual(LatLong location, Instant time) {

            //See: https://en.wikipedia.org/wiki/Studentized_residual

            //Related topics: Standardized residuals, Studentized residuals, and Studentized deleted residuals

            double latitudePrediction = latRegression.predict(time.toEpochMilli());
            double longitudePrediction = longRegression.predict(time.toEpochMilli());

            // The residuals are the differences between the "actual observation" and the regression's prediction
            double latResidual = location.latitude() - latitudePrediction;
            double longResidual = location.longitude() - longitudePrediction;

            // Now we rescale these residual so we can decide if the error is "big" or "small"

            // Note: "The residuals, unlike the errors, do not all have the same variance: the
            // variance decreases as the corresponding x-value gets farther from the average x-value"

            // Technically, this is a "SEMI-studentized Residual" = residual_i / sqrt(mse)
            // IF! we accounted for the sample's leverage we'd have a full "studentized residual"
            double studentized_lat_residual = abs(latResidual / sqrt(latRegression.getMeanSquareError()));
            double studentized_long_residual = abs(longResidual / sqrt(longRegression.getMeanSquareError()));

            // using hypot to form a reasonable combination of these student-t tests
            return hypot(studentized_lat_residual, studentized_long_residual);
        }
    }
}
