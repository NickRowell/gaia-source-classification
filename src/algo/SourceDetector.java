package algo;

import java.util.List;

import dm.Source;
import dm.Window;
import util.LocalBkgUtils;

/**
 * Base class for MultipleSourceDetector algorithms. Defines the interface that implementations should adhere to.
 *
 * TODO: set the flux threshold for faint sources to a physically meaningful level, e.g. magnitude 21 (would need to
 * know the integration time to do this). The current value in electrons was chosen empirically.
 *
 * TODO: allow integer sample array to be passed in, and use nominal Gaia gain value to scale ADU to electrons. Nominal
 * gains by instrument (obtained from GPDB web interface; :Satellite:SM:VideoChain_Gain_LSBPerElectron etc): SM: 0.2566
 * AF: 0.2566 BP: 0.24876 RP: 0.27222 RVS: 1.90551
 *
 * @author nrowell
 * @version $Id: SourceDetectorBase.java 471308 2015-12-14 12:09:36Z nrowell $
 */
public abstract class SourceDetector {

    /**
     * Source detection threshold (sigmas above the background level).
     */
    public static final double SOURCE_DETECTION_THRESHOLD_SIGMAS = 10.0;

    /**
     * Integrated flux threshold for valid sources [e-]. Sources with an integrated flux lower than this will be assumed
     * to be noise and culled from the set of detected sources.
     */
    public static final double FAINT_SOURCE_FLUX_THRESHOLD_E = 100.0;

    /**
     * Detects and classifies sources in the window, and returns a List of all the sources found. Works for both 1D and
     * 2D windows, though the classifications available are restricted for 1D windows.
     *
     * The samples must be bias-subtracted, or alternatively, the background level passed in must include the bias.
     * Essentially this is the value relative to which sources are detected.
     *
     * @param samples
     *            Array of all samples (AC-packed) [e-]
     * @param alLength
     *            Length of samples window in AL direction
     * @param acLength
     *            Length of samples window in AC direction
     * @param alPixPerSample
     *            Number of pixels in a sample, in the AL direction
     * @param acPixPerSample
     *            Number of pixels in a sample, in the AC direction
     * @param bkg
     *            Background level and error (STD)
     * @return List containing the (classified) Sources detected in the window
     */
    public abstract List<Source> getSources(float[] samples, int alLength, int acLength, int alPixPerSample,
            int acPixPerSample, double[] bkg);

    /**
     * Alternative interface to {@link #getSources(double[], int, int, int, int, double[], boolean)} that computes an
     * empirical estimate of the local background and error for use in the multiple source detection.
     *
     * The samples do not need to be debiased and background subtracted: sources are detected using a locally estimated
     * background level that combines both the bias level and the stray light background.
     *
     * @param samples
     *            Array of all samples (AC-packed) [e-].
     * @param alLength
     *            Length of samples window in AL direction
     * @param acLength
     *            Length of samples window in AC direction
     * @param alPixPerSample
     *            Number of pixels in a sample, in the AL direction
     * @param acPixPerSample
     *            Number of pixels in a sample, in the AC direction
     * @return List containing the (classified) Sources detected in the window
     */
    public List<Source> getSources(Window window) {
    	
    	float[] samples = window.samples;
    	int alLength = window.alSamples;
    	int acLength = window.acSamples;
    	int alPixPerSample = window.alSampleSize;
    	int acPixPerSample = window.acSampleSize;
    	
        final double[] bkgAndError = LocalBkgUtils.estimateBackgroundAndError(samples);
        return getSources(samples, alLength, acLength, alPixPerSample, acPixPerSample, bkgAndError);
    }
}