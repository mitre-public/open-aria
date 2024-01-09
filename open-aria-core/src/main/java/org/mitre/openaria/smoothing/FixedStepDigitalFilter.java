
package org.mitre.openaria.smoothing;

/**
 * A {@code FixedStepDigitalFilter} takes as input a signal and can return the smoothed signal and
 * the first derivative of the smoothed signal. The output signal size is equal to the input signal
 * size ({@code input.length == output.length} returns true.
 *
 * <p> <b>Note: The input signals must have a fixed time step!</b> </p>
 * <p>
 * For generality and efficiency, the input and output signals are simple double arrays, returning
 * the signal in the same units ({@link #smooth(double[])}) or the original units divided by time
 * ({@link #smoothDerivative(double[])}), using the provided time step to scale appropriately.
 */
public interface FixedStepDigitalFilter {

    double[] smooth(double[] input);

    double[] smoothDerivative(double[] input);

}
