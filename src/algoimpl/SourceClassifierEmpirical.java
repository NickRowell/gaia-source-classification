package algoimpl;

import java.util.Collections;
import java.util.logging.Logger;

import algo.SourceClassifier;
import dm.Source;
import dm.Source.Type;

/**
 * Implementation of {@link SourceClassifier} that uses simple heuristic cuts on the shape and flux
 * statistics to classify sources according to the various types.
 * 
 * @author nrowell
 */
public class SourceClassifierEmpirical implements SourceClassifier {

    /**
     * The Logger
     */
    protected static Logger logger = Logger.getLogger(SourceClassifierEmpirical.class.getCanonicalName());

    /**
     * Minimum peak sample for cosmic rays [e-]
     */
    private final double PEAK_FLUX_THRESHOLD = 1500.0;

    /**
     * Threshold on the peak-to-neighbour sample ratio to separate stellar and cosmic ray sources. This threshold is
     * applied to unbinned samples in 2D windows and all 1D windows.
     */
    private final double PEAK_NEIGHOUR_RATIO_THRESHOLD_DEFAULT = 10.0;

    /**
     * Threshold on the peak-to-neighbour sample ratio to separate stellar and cosmic ray sources. This threshold is
     * applied to samples with 2x1 pixel binning [AF1 class 0].
     */
    private final double PEAK_NEIGHOUR_RATIO_THRESHOLD_2x1 = 20.0;

    /**
     * Threshold on the peak-to-neighbour sample ratio to separate stellar and cosmic ray sources. This threshold is
     * applied to samples with 2x2 pixel binning [SM class 0].
     */
    private final double PEAK_NEIGHOUR_RATIO_THRESHOLD_2x2 = 20.0;

    /**
     * Threshold on the peak-to-neighbour sample ratio to separate stellar and cosmic ray sources. This threshold is
     * applied to samples with 4x4 pixel binning [SM class 1].
     */
    private final double PEAK_NEIGHOUR_RATIO_THRESHOLD_4x4 = 55.0;

    /**
     * Threshold on the largest eigenvalue for a source to be classified as a spike.
     */
    private final double LARGEST_EIGENVALUE_MIN_SPIKE = 15.0;

    /**
     * Threshold on the eigenvalue ratio for a source to be classified as a spike
     */
    private final double EIGENVALUE_RATIO_MIN_SPIKE = 3.0;

    /**
     * Threshold on the angle between the spike and either axis for the spike to be assigned AC/AL/DIAGONAL. E.g. if the
     * spike is within this angle of the AC axis it will be assigned an AC orientation.
     */
    private final double DIAGONAL_SPIKE_THRESHOLD = Math.toRadians(10.0);

    /**
     * Default constructor.
     */
    public SourceClassifierEmpirical() {
    	// Nothing to intialise
    }
    
    /**
     * Get the Type classification of the source.
     *
     * @param source
     *            The Source to classify.
     * @return The Type classification for this Source
     */
    public Type classifySource(Source source) {

        // The Type of the source
        Type classification = Type.UNKNOWN;

        // Classify 2D windows as spikes first
        if (!source.getIs1D()) {
            classification = getSpikeClassification(source);
        }

        // If this wasn't classified as a spike, or is 1D, then further classify as stellar/cosmic:
        if (classification == Type.UNKNOWN) {
            classification = getCosmicStellarClassification(source);
        }

        // Don't trust cosmic rays in the first TDI line
        if (classification == Type.COSMIC) {
            Collections.sort(source.getSamples());
            if (source.getSamples().get(0).getAl() == source.getAlWinSize() - 1) {
                classification = Type.UNKNOWN;
            }
        }

        return classification;
    }

    /**
     * Classifies the Source as either a horizontal, vertical or diagonal spike (or unknown) in origin, based on the
     * flux-weighted sample distribution.
     * <p>
     * The method implemented here is more sophisticated than that in GAIA-C5-TN-IOA-FVL-145-D (which only checks the
     * dispersion of the marginal AL & AC coordinates). Here, we measure the flux-weighted dispersion matrix for the AC
     * and AL coordinates, then compute the eigenvalue decomposition. The eigenvalues express the degree of elongation
     * of the source along the major and minor axes. Empirically, diffraction spikes have one eigenvalue very large, and
     * the other smaller. Note that cosmic ray sources can have a large ratio of largest to smallest eigenvalue, so we
     * need an absolute threshold as well as a threshold on the ratio.
     *
     * A rough table of classification is as follows:
     * <p>
     * <table>
     * <col width="25%"/> <col width="25%"/> <col width="50%"/><thead>
     * <tr>
     * <th align="left">L1</th>
     * <th align="left">L1/L2</th>
     * <th align="left">Classification</th>
     * </tr>
     * <thead> <tbody>
     * <tr>
     * <td>Large</td>
     * <td>~=1</td>
     * <td>Unknown</td>
     * </tr>
     * <tr>
     * <td>Large</td>
     * <td>>~5</td>
     * <td>Spike</td>
     * </tr>
     * <tr>
     * <td>Small</td>
     * <td>~=1</td>
     * <td>Unknown (likely stellar)</td>
     * </tr>
     * <tr>
     * <td>Small</td>
     * <td>>>1</td>
     * <td>Unknown (likely cosmic)</td>
     * </tr>
     * </tbody>
     * </table>
     *
     *
     * <p>
     * See GAIA-C5-TN-IOA-FVL-145-D.
     *
     * @param source
     *            The Source to classify.
     * @return The classification; either SPIKE_HORIZONTAL, SPIKE_VERTICAL, SPIKE_DIAGONAL or UNKNOWN
     */
    private Type getSpikeClassification(Source source) {

        final double[] eigs = source.getEigenvalues();
        final double orientation = source.getOrientation();

        Type type = Type.UNKNOWN;

        // Health checks on statistics
        if (eigs == null || Double.isNaN(orientation) || Double.isNaN(eigs[0]) || Double.isNaN(eigs[1])) {
            return type;
        }

        // In SM, there are many false positive diffraction spike classifications because of the longer
        // windows in the AL direction. For now, I have disabled classification of diffraction spikes in
        // SM until a more discriminative method can be found.
        if (source.getAlWinSize() > 18) {
            return type;
        }

        // Get the ratio of the eigenvalues
        final double eigRatio = eigs[0] / eigs[1];

        // First check the magnitude of the largest eigenvalue
        if (eigs[0] < LARGEST_EIGENVALUE_MIN_SPIKE) {
            // Source is too compact to be a spike
            if (eigRatio < EIGENVALUE_RATIO_MIN_SPIKE) {
                // Likely Stellar
                type = Type.UNKNOWN;
            } else {
                // Possible cosmic
                type = Type.UNKNOWN;
            }
        } else {

            if (eigRatio < EIGENVALUE_RATIO_MIN_SPIKE) {
                // Source is large but round - not a spike (or anything sensible)
                type = Type.UNKNOWN;
            } else {
                // Source is large and linear - we found a spike!
                // Further classify the type by checking the orientation
                // Check orientation wrt AL direction to determine which classification it is
                if (orientation < DIAGONAL_SPIKE_THRESHOLD) {
                    type = Type.SPIKE_AL;
                } else if (orientation > Math.PI / 2.0 - DIAGONAL_SPIKE_THRESHOLD) {
                    type = Type.SPIKE_AC;
                } else {
                    type = Type.SPIKE_DIAGONAL;
                }
            }
        }

        return type;
    }

    /**
     * Classifies the Source as either cosmic ray or stellar (or unknown) in origin, based on the flux ratio between the
     * strongest sample and it's immediate neighbours.
     * <p>
     * Cosmics are indicated by Sources for which there is a large ratio between the largest sample and it's immediate
     * neighbours.
     *
     *
     * <p>
     * <table>
     * <col width="25%"/> <col width="25%"/> <col width="50%"/><thead>
     * <tr>
     * <th align="left">Peak flux</th>
     * <th align="left">Peak/Neighbours</th>
     * <th align="left">Classification</th>
     * </tr>
     * <thead> <tbody>
     * <tr>
     * <td>Large</td>
     * <td>~=1</td>
     * <td>Stellar (bright)</td>
     * </tr>
     * <tr>
     * <td>Large</td>
     * <td>>>1</td>
     * <td>Cosmic</td>
     * </tr>
     * <tr>
     * <td>Small</td>
     * <td>~=1</td>
     * <td>Stellar (faint)</td>
     * </tr>
     * <tr>
     * <td>Small</td>
     * <td>>>1</td>
     * <td>Unknown (likely noise)</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p>
     * See GAIA-C5-TN-IOA-FVL-145-D.
     *
     * @param source
     *            The Source to classify.
     * @return The classification; either COSMIC, STELLAR or UNKNOWN.
     */
    private Type getCosmicStellarClassification(Source source) {

        // The ratio between the flux of the strongest sample and the avergae of it's immediate neighbours
        final double fluxRatio = source.getFluxRatio();
        final double peakFlux = source.getPeakFlux();

        // Get the flux threshold to apply
        double cosmicStellarFluxRatioThreshold = PEAK_NEIGHOUR_RATIO_THRESHOLD_DEFAULT;

        if (source.getAlSampleSize() == 1 && source.getAcSampleSize() == 2) {
            cosmicStellarFluxRatioThreshold = PEAK_NEIGHOUR_RATIO_THRESHOLD_2x1;
        } else if (source.getAlSampleSize() == 2 && source.getAcSampleSize() == 2) {
            cosmicStellarFluxRatioThreshold = PEAK_NEIGHOUR_RATIO_THRESHOLD_2x2;
        } else if (source.getAlSampleSize() == 4 && source.getAcSampleSize() == 4) {
            cosmicStellarFluxRatioThreshold = PEAK_NEIGHOUR_RATIO_THRESHOLD_4x4;
        }

        Type type = Type.UNKNOWN;

        // Health checks on statistics
        if (Double.isNaN(fluxRatio) || Double.isNaN(peakFlux)) {
            return type;
        }

        if (peakFlux > PEAK_FLUX_THRESHOLD) {

            if (fluxRatio > cosmicStellarFluxRatioThreshold) {
                // Bright, compact source: a cosmic ray
                type = Type.COSMIC;
            } else {
                // Bright, diffuse source: a bright star
                type = Type.STELLAR;
            }
        } else {

            if (fluxRatio > cosmicStellarFluxRatioThreshold) {
                // Faint, compact source: likely noise
                type = Type.UNKNOWN;
            } else {
                // Faint, diffuse source: a faint star
                type = Type.STELLAR;
            }
        }

        return type;
    }
}