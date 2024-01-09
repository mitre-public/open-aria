

package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.math3.linear.MatrixUtils.inverse;
import static org.apache.commons.math3.util.FastMath.pow;

import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;


/**
 * The <a href="https://en.wikipedia.org/wiki/Savitzky–Golay_filter">Savitzky–Golay Filter</a> can
 * perform smoothing and N-th order derivatives on digital signals to increase the signal-to-noise
 * ratio, via convolution. Importantly, when the data points are equally spaced in time (i.e.
 * constant time-step), analytical solutions to the least-squares problem can be found, represented
 * as convolution coefficients. This can save significant computational time & effort.
 *
 * <p>Note that the convolution coefficients (with require some matrix inversions) are computed when
 * the filter is created. If this is occurring during smoothing the computation will occur once per
 * thread.
 *
 * <p>The size of the {@link #weights} array determines the number of points in the filter window.
 * Obviously, the weights indicated the relative importance of the points in the window. There must
 * be an odd number of points in the window.
 */
public class SavitzkyGolayFilter implements FixedStepDigitalFilter {

    /**
     * The order of interpolation is either Linear (1), Quadratic (2), Cubic (3), etc.
     */
    private static final int DEFAULT_ORDER_OF_INTERPOLATION = 1;
    private static final double[] DEFAULT_WEIGHTS = new double[]{1, 1, 1, 2, 2, 3, 3, 3, 2, 2, 1, 1, 1};

    private final int orderOfInterpolation;
    private final double[] weights;
    private final double[] smoothCoeff;
    private final double[] derivCoeff;
    private final int offsetFromWindowCenter;
    private final double timeStep;

    /**
     * Initialize the filter with the timeStep of the incoming data. This is especially relevant for
     * the {@link #smoothDerivative(double[])} method, as the time step is needed to normalize the
     * data (i.e. return it in the correct units).
     *
     * <p>For example, if the time step is in minutes, and the incoming data has units of feet, the
     * {@link #smoothDerivative(double[])} method will return results in units of ft/min.
     */
    public SavitzkyGolayFilter(double timeStep) {
        this(timeStep, DEFAULT_ORDER_OF_INTERPOLATION, DEFAULT_WEIGHTS);
    }

    public SavitzkyGolayFilter(double timeStep, int orderOfInterpolation, double[] weights) {
        checkArgument(timeStep > 0, "timeStep must be strictly positive");
        checkArgument(orderOfInterpolation >= 1, "order of interpolation must be at least 1");
        checkArgument(weights.length % 2 != 0, "weight array must be odd");

        this.timeStep = timeStep;
        this.orderOfInterpolation = orderOfInterpolation;
        this.weights = weights;

        this.smoothCoeff = computeSmoothingCoefficients(weights);
        this.derivCoeff = computeDerivativeCoefficients(weights);
        this.offsetFromWindowCenter = computeOffsetFromWindowCenter(weights);
    }

    @Override
    public double[] smooth(double[] input) {

        if (input.length < weights.length) {
            return input;
        }

        double[] smoothed = new double[input.length];
        int halfWindowFloored = weights.length / 2; // we want to exclude the center point

        fillSmoothedLeftSide(smoothed, input, halfWindowFloored);
        fillSmoothedRightSide(smoothed, input, halfWindowFloored);

        for (int i = halfWindowFloored; i < input.length - halfWindowFloored; i++) {
            for (int windowIndex = 0; windowIndex < smoothCoeff.length; windowIndex++) {
                smoothed[i] += smoothCoeff[windowIndex] * input[i + windowIndex + offsetFromWindowCenter];
            }
        }
        return smoothed;
    }


    private void fillSmoothedLeftSide(double[] smoothed, double[] raw, int halfWindowFloored) {

        for (int i = 0; i < halfWindowFloored; i++) {
            smoothed[i] = raw[i];
        }
    }

    private void fillSmoothedRightSide(double[] smoothed, double[] raw, int halfWindowFloored) {

        int length = raw.length;
        for (int i = halfWindowFloored; i > 0; i--) {
            smoothed[length - i] = raw[length - i];
        }
    }

    @Override
    public double[] smoothDerivative(double[] input) {

        if (input.length < weights.length) {
            return averageDerivativeForVeryShortTrack(input);
        }

        double[] smoothed = new double[input.length];
        int halfWindowFloored = weights.length / 2; // we want to exclude the center point

        for (int i = halfWindowFloored; i < input.length - halfWindowFloored; i++) {
            for (int windowIndex = 0; windowIndex < smoothCoeff.length; windowIndex++) {
                smoothed[i] += derivCoeff[windowIndex] * input[i + windowIndex + offsetFromWindowCenter];
            }
            smoothed[i] = smoothed[i] / timeStep;
        }

        fillSmoothDerivativeLeftSide(smoothed, halfWindowFloored);
        fillSmoothDerivativeRightSide(smoothed, halfWindowFloored);

        return smoothed;
    }

    private void fillSmoothDerivativeLeftSide(double[] smoothed, int halfWindowFloored) {

        for (int i = 0; i < halfWindowFloored; i++) {
            smoothed[i] = smoothed[halfWindowFloored];
        }
    }

    private void fillSmoothDerivativeRightSide(double[] smoothed, int halfWindowFloored) {

        int length = smoothed.length;
        for (int i = halfWindowFloored; i > 0; i--) {
            smoothed[length - i] = smoothed[length - (halfWindowFloored + 1)];
        }
    }


    private double[] averageDerivativeForVeryShortTrack(double[] input) {

        double[] smoothed = new double[input.length];

        double delta = 0.0;
        for (int i = 0; i < input.length - 1; i++) {
            delta += input[i + 1] - input[i];
        }
        delta = (delta / (input.length - 1.0)) / timeStep;

        for (int i = 0; i < input.length; i++) {
            smoothed[i] = delta;
        }
        return smoothed;
    }

    private double[] computeSmoothingCoefficients(double[] weights) {

        RealMatrix C = solveForCoefficients(weights);

        return C.getRow(0);
    }


    private double[] computeDerivativeCoefficients(double[] weights) {

        RealMatrix C = solveForCoefficients(weights);

        return C.getRow(1);
    }

    /**
     * See https://en.wikipedia.org/wiki/Savitzky%E2%80%93Golay_filter#Derivation_of_convolution_coefficients
     *
     * <p>Example for a linear fit (hence 2 columns in J), with a window size of 5 elements, and the
     * weights being 1, 2, 5, 2, 1:
     * <pre>
     * J =
     * [1, -2;
     *  1, -1;
     *  1, 0;
     *  1, 1;
     *  1, 2];
     *
     * W = diag([1, 2, 5, 2, 1]);
     *
     * C = inv(J'*W*J)*(J')*W;
     *
     * C =
     * 0.0909    0.1818    0.4545    0.1818    0.0909
     * -0.1667   -0.1667         0    0.1667    0.1667
     * </pre>
     *
     * <p>The first row of C is for smoothing The second row of C is for computing the smoothed first
     * derivative The third row of C (if present) is for the smoothed second derivative, etc.
     */
    private RealMatrix solveForCoefficients(double[] weights) {

        int windowSize = weights.length;

        if (windowSize % 2 == 0) {
            throw new IllegalStateException("There must be an odd number of SavitzkyGolayFilter weights!");
        }

        double[] z = new double[windowSize];
        double offset = (1 - windowSize) / 2;

        for (int i = 0; i < windowSize; i++) {
            z[i] = i + offset;
        }

        int cols = orderOfInterpolation + 1;

        double[][] j = new double[windowSize][cols];

        for (int row = 0; row < windowSize; row++) {
            for (int column = 0; column < cols; column++) {
                j[row][column] = pow(z[row], column);
            }
        }

        double[][] w = new double[windowSize][windowSize];
        for (int row = 0; row < windowSize; row++) {
            w[row][row] = weights[row];
        }

        BlockRealMatrix J = new BlockRealMatrix(j);
        BlockRealMatrix W = new BlockRealMatrix(w);

        //https://en.wikipedia.org/wiki/Savitzky%E2%80%93Golay_filter#Derivation_of_convolution_coefficients
        RealMatrix leftTerm = inverse(J.transpose().multiply(W).multiply(J)); // (J^T*W*J)^-1
        RealMatrix rightTerm = (J.transpose()).multiply(W); //(J^T*W)

        return leftTerm.multiply(rightTerm); //return coefficient Matrix C
    }

    /**
     * E.g. if the window has 5 points, the center will be at index 2 and the offset is therefore
     * -2; i.e. the left-most point has relative index of -2, the next has -1, the center is at 0,
     * the right-of-center is +1, and the right-most is +2.
     */
    private int computeOffsetFromWindowCenter(double[] weights) {

        return (1 - weights.length) / 2;
    }
}
