
package org.mitre.openaria.smoothing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class SavitzkyGolayFilterTest {

    @Test
    public void Smooth_FromShortTrack_ReturnSameTrack() {
        SavitzkyGolayFilter test = new SavitzkyGolayFilter(2.0);

        double[] input = new double[]{10.0, 11.0, 12.0, 13.0, 15.0};
        double[] actual = test.smooth(input);

        assertThat(actual.length, equalTo(input.length));

        for (int i = 0; i < input.length; i++) {
            assertThat(actual[i], closeTo(input[i], 1E-3));
        }
    }

    @Test
    public void Derivative_FromShortTrack_ReturnAverageDerivative() {
        SavitzkyGolayFilter test = new SavitzkyGolayFilter(3.0); // note the time step

        double[] input = new double[]{10.0, 13.0, 16.0, 18.0, 21.0};
        double[] actual = test.smoothDerivative(input);

        assertThat(actual.length, equalTo(input.length));

        for (int i = 0; i < input.length; i++) {
            assertThat(actual[i], closeTo(0.9167, 1E-3));
        }
    }

    @Test
    public void Smooth_FromFakeTrackWithOutlier_RemoveBumps() {
        SavitzkyGolayFilter test = new SavitzkyGolayFilter(1.0);

        double[] input = new double[]{
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 13.0, // <-- outlier
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0
        };
        double[] actual = test.smooth(input);

        assertThat(actual.length, equalTo(input.length));

        for (int i = 0; i < input.length; i++) {
            assertThat(actual[i], closeTo(10.0, 1.0));
        }
    }

    @Test
    public void Derivative_FromFakeTrackWithOutlier_RemoveBumps() {
        SavitzkyGolayFilter test = new SavitzkyGolayFilter(1.0);

        double[] input = new double[]{
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 13.0, // <-- outlier
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0
        };
        double[] actual = test.smoothDerivative(input);

        assertThat(actual.length, equalTo(input.length));

        for (int i = 0; i < input.length; i++) {
            assertThat(actual[i], closeTo(0.0, 0.5));
        }
    }

    @Test
    public void Derivative_FromFakeTrackWithSymmetricOutliers_RemoveBumps() {
        SavitzkyGolayFilter test = new SavitzkyGolayFilter(1.0);

        double[] input = new double[]{
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0,
            13.0, 16.0, 13.0,  // <-- outlier points make a symmetric "triangle", center at index = 8
            10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0
        };
        double[] actual = test.smoothDerivative(input);

        assertThat(actual.length, equalTo(input.length));
        assertThat(actual[8], closeTo(0.0, 1E-3));
    }

    @Test
    public void Smooth_FromEmptyTrack_ReturnEmptyTrack() {

        SavitzkyGolayFilter test = new SavitzkyGolayFilter(1.0);
        double[] actual = test.smooth(new double[0]);

        assertThat(actual.length, equalTo(0));
    }

    @Test
    public void Derivative_FromEmptyTrack_ReturnEmptyTrack() {

        SavitzkyGolayFilter test = new SavitzkyGolayFilter(1.0);
        double[] actual = test.smoothDerivative(new double[0]);

        assertThat(actual.length, equalTo(0));
    }
}