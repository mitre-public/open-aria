
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * A VerticalOutlierDetector can perform two highly related tasks. It can be used as a DataCleaner
 * that adjusts the altitude value of Points in a Track. A VerticalOutlierDetector can also be used
 * to just identify Points with outlying altitude values.
 */
public class VerticalOutlierDetector<T> implements DataCleaner<Track<T>> {

    private final int REQUIRED_SAMPLE_SIZE = 7;

    private final double MIN_CORRECTABLE_ALTITUDE_ERROR = 300; //in feet

    /**
     * Find the Points in the input track with outlying altitude values;
     *
     * @param track A Track
     *
     * @return The set of Point in this Track with outlying altitude values.
     */
    public ArrayList<AnalysisResult<T>> getOutliers(Track<T> track) {

        // the stream is wonky due to the raw type, probably could be improved

        return track.points().stream()
            .map(point -> analyzePoint(point, track))
            .filter(analysisResult -> analysisResult.isOutlier())
            .collect(toCollection(ArrayList::new));
    }

    /**
     * Create a cleaned version of the input track that corrects the altitude value of Point that
     * were designated vertical outliers.
     *
     * @param inputTrack A Track
     *
     * @return An Optional Track that contains Points with correct altitude values. The Optional
     *     Track will always exist because this smoother does not reduce the number of Points in the
     *     inputTrack. The result is returned as an Optional so that this class can properly
     *     implement the DataCleaner interface
     */
    @Override
    public Optional<Track<T>> clean(Track<T> inputTrack) {

        ArrayList<AnalysisResult<T>> outliers = getOutliers(inputTrack);

        TreeSet<Point<T>> points = new TreeSet<>(inputTrack.points());
        points.removeAll(outliers.stream().map(ar -> ar.originalPoint).toList());
        points.addAll(outliers.stream().map(ar -> ar.correctedPoint()).toList());

        return Optional.of(Track.of(points));
    }

    /**
     * This method analyzes the altitude of a single Point within a Track. The analysis is designed
     * to determine if a point has an outlying altitude value and, if so, what a good correction for
     * that altitude value would be.
     * <p>
     * The determination is made by comparing two Simple Linear Regressions. One regression uses
     * time and altitude data from ONLY neighboring Point. The other regression also includes the
     * testPoint's (time, altitude) data-point.
     * <p>
     * A Point is declared an outlier if adding the TestPoint's data causes a large drop in the
     * rSquared of the regression AND the Mean Squared Error of the full regression is "relevant".
     *
     * @param testPoint A single point within the track
     * @param track     The source track
     *
     * @return An AnalysisResult object that summarizes the altitude analysis.
     */
    private AnalysisResult<T> analyzePoint(Point<T> testPoint, Track<T> track) {

        Collection<Point<T>> pointsNearby = track.kNearestPoints(
            testPoint.time(),
            REQUIRED_SAMPLE_SIZE
        );

        if (pointsNearby.size() < REQUIRED_SAMPLE_SIZE) {
            //When the sample size is small do not declare outliers and do not provide predictions
            return new AnalysisResult<>(testPoint);
        }

        SimpleRegression regression = regressionWithoutTestPoint(pointsNearby, testPoint);

        //use the regression to predict the "correct" altitude
        double predictedAltitude = regression.predict(testPoint.time().toEpochMilli());
        double altitudeError = abs(predictedAltitude - testPoint.altitude().inFeet());

        //rSquare will be NaN if the error in the regression is exactly 0
        double rSquaredWithout = (Double.isNaN(regression.getRSquare())
            ? 1.0
            : regression.getRSquare());

        //now add the testPoint to the regression
        regression.addData(
            testPoint.time().toEpochMilli(),
            testPoint.altitude().inFeet()
        );

        //rSquare will be NaN if the error in the regression is exactly 0
        double rSquared = (Double.isNaN(regression.getRSquare())
            ? 1.0
            : regression.getRSquare());

        boolean largeDropInR = (rSquaredWithout - rSquared) > .5;

        boolean isOutlier = (largeDropInR && altitudeError > MIN_CORRECTABLE_ALTITUDE_ERROR);

        return new AnalysisResult<>(testPoint, isOutlier, predictedAltitude);
    }

    /**
     * This class is not meant to be used externally.
     */
    public static class AnalysisResult<T> {

        private final Point<T> originalPoint;
        private final boolean isOutlier;
        private final Distance correctedAltitude;

        AnalysisResult(Point<T> originalPoint, boolean isOutlier, double correctedAltitude) {
            this.originalPoint = checkNotNull(originalPoint);
            this.isOutlier = isOutlier;

            //only save prediction values for points that are considered outliers.
            this.correctedAltitude = (isOutlier)
                ? Distance.ofFeet(correctedAltitude)
                : null;
        }

        AnalysisResult(Point<T> originalPoint) {
            this(originalPoint, false, Double.NaN);
        }

        public Point<T> originalPoint() {
            return originalPoint;
        }

        public boolean isOutlier() {
            return isOutlier;
        }

        public Distance correctedAltitude() {
            return correctedAltitude;
        }

        public Point<T> correctedPoint() {
            return Point.builder(originalPoint).altitude(correctedAltitude).build();
        }
    }

    /**
     * Create a Linear Regression that from all these input points except for the testPoint.
     *
     * @param localPoints A set of points that are near the test point
     * @param testPoint   A point that will be excluded from the regression
     *
     * @return A simple linear regression that regresses altitude vs. time from the points supplied.
     */
    private SimpleRegression regressionWithoutTestPoint(Collection<Point<T>> localPoints, Point<T> testPoint) {

        SimpleRegression regression = new SimpleRegression();

        for (Point<T> localPoint : localPoints) {

            //don't include the test point
            if (localPoint == testPoint) {
                continue;
            }

            //add all other (time , altitude) data points to the regression
            regression.addData(
                localPoint.time().toEpochMilli(),
                localPoint.altitude().inFeet()
            );
        }

        return regression;
    }
}
