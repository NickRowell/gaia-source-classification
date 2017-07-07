package util;

import java.util.Arrays;

/**
 * Methods to estimate the local background for a window.
 *
 * @author nrowell
 * @version $Id: LocalBkgUtils.java 470593 2015-12-09 18:54:47Z mdavidso $
 */
public class LocalBkgUtils {

    /**
     * Method obtains an empirical estimate of the local background for the samples, from the median and MAD of the
     * lowest four samples in the array. This will give a spurious result if there are eliminated samples present, but
     * in this case these windows are expected to fail other tests (such as bad IPD) so that shouldn't be important.
     *
     * @param samples
     *            Array of all samples
     * @return The background [0] and background error [1]
     */
    public static double[] estimateBackgroundAndError(float[] samples) {

        // The number of lowest value elements to examine to obtain the
        // background estimate. We use a different number for each window
        // size in order to get a more robust estimate for the larger windows
        int n = 4;

        if (samples.length == 6) {
            // Class 2 windows in AF1
            n = 2;
        } else if (samples.length == 12 || samples.length == 18) {
            // Class 1 & 2 windows in AF2-9; class 1 windows in AF1
            n = 4;
        } else if (samples.length == 18 * 12) {
            // Class 0 windows in AF2-9
            n = 31;
        } else if (samples.length == 20 * 3) {
            // Class 1 windows in SM
            n = 31;
        } else if (samples.length == 40 * 6) {
            // Class 0 windows in SM
            n = 31;
        } else if (samples.length == 108) {
            // Class 0 windows in AF1
            n = 11;
        }

        // Sort samples into ascending order. Must clone array first to avoid
        // scrambling the original array
        final float[] sortedSamples = samples.clone();
        Arrays.sort(sortedSamples);

        // Copy the lowest values out to a new array
        final float[] lowestNSamples = Arrays.copyOf(sortedSamples, n);

        // Median of the lowest N samples
        final double median = LocalBkgUtils.median(lowestNSamples);

        // Absolute deviations of the lowest N samples
        final double[] dev = new double[n];
        for (int i = 0; i < n; i++) {
            dev[i] = Math.abs(lowestNSamples[i] - median);
        }

        // RMS is probably more appropriate for the background, because we tend to
        // see multiple samples with the same value at the low end of the range.
        final double rms = LocalBkgUtils.rms(dev);

        return new double[] { median, rms };
    }

    /**
     * Computes the RMS value of the elements in the array.
     *
     * @param samples
     *            The values to process
     * @return The root-mean-square value of the array
     */
    private static double rms(double[] samples) {
        double sumSq = 0.0;
        for (final double sample : samples) {
            sumSq += sample * sample;
        }
        sumSq /= samples.length;
        return Math.sqrt(sumSq);
    }

    /**
     * Computes the median value of the samples in the array. The order of the elements is changed by this operation.
     *
     * @param samples
     *            The array of samples. Note that the order is changed by this method.
     * @return The median value of the entries in the samples array.
     */
    private static float median(float[] samples) {
        if (samples.length == 0) {
            return Float.NaN;
        }

        // Sort into ascending order
        Arrays.sort(samples);

        // Get median value
        final int n = samples.length;
        float median = Float.NaN;

        if (n == 1) {
            median = samples[0];
        } else if (n % 2 == 0) {
            // Even number of elements: median is average of central two
            median = (float)((samples[n / 2 - 1] + samples[n / 2]) / 2.0);
        } else {
            // Odd number of elements: median is the central element
            median = samples[(n - 1) / 2];
        }
        return median;
    }
}
