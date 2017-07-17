package algoimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import algo.SourceDetector;
import dm.Sample;
import dm.Source;
import util.Direction;
import util.FloatList;

/**
 * Implementation of the {@link SourceDetector} class that is based on the watershed segmentation algorithm.
 *
 * @author nrowell
 * @version $Id: SourceDetectorWatershedSegmentation.java 471308 2015-12-14 12:09:36Z nrowell $
 */
public class SourceDetectorWatershedSegmentation extends SourceDetector {

    /**
     * Logger
     */
    protected static Logger logger = Logger.getLogger(SourceDetectorWatershedSegmentation.class.getCanonicalName());

    /**
     * When performing the watershed segmentation, we draw watershed lines between neighbouring samples when the next
     * sample has a higher intensity level than the last one (indicating the start of the next source). However, to
     * allow for noisy samples we include a tolerance on the intensity change from one sample to the next to avoid noise
     * introducing spurious sources. This constant gives the tolerance level in terms of sigmas: a neighbouring sample
     * will only be considered not connected to the current sample is it's intensity level is more than
     * {@link #CONNECTIVITY_THRESHOLD_SIGMAS} sigmas above the current sample (with the sigma computed from both samples
     * assuming Poisson statistics).
     */
    private static final double CONNECTIVITY_THRESHOLD_SIGMAS = 2.0;

    /**
     * This implementation of {@link SourceDetector#getSources(double[], int, int, int, int, double[], boolean)} is
     * based on the watershed segmentation algorithm.
     */
    @Override
    protected List<Source> getSources(float[] samples, int alLength, int acLength, int alPixPerSample,
            int acPixPerSample, double[] bkg) {

        // Get (unclassified) sources
        final List<Source> sources = getWatershedSegmentation(samples, alLength, acLength, alPixPerSample,
                acPixPerSample, bkg[0], bkg[1]);

        // Compute some statistics of the source intensity distribution that will aid in source classification.
        for (final Source source : sources) {
            computeSourceFluxStatistics(source, samples, alLength, acLength, bkg);
            computeSourceShapeStatistics(source, samples, alLength, acLength, alPixPerSample, acPixPerSample, bkg);
        }

        // Extract bright sources from array
        final List<Source> brightSources = new LinkedList<>();
        for (final Source source : sources) {
            if (source.getFlux() > SourceDetector.FAINT_SOURCE_FLUX_THRESHOLD_E) {
                brightSources.add(source);
            }
        }

        return brightSources;
    }

    /**
     * Static inner class so that we can use arrays of lists to record the source assignments for each sample in the
     * watershed algorithm below.
     *
     * @author nrowell
     * @version $Id: SourceDetectorWatershedSegmentation.java 471308 2015-12-14 12:09:36Z nrowell $
     */
    private static class LabelsList extends LinkedList<Integer> {

        /**
         * The serial version UID.
         */
        private static final long serialVersionUID = -6828772047953976297L;
    }

    /**
     * Full watershed segmentation. Merged sources above the threshold level are detected and segmented along the
     * 'watershed lines', i.e. the lowest points between them. The watershed segmentation is mainly achieved by
     * processing connected samples in intensity order so that when a new neighouring sample is found that has an
     * intensity larger than it's neighbour in the source, we know it's not connected to the current source.
     *
     * The segmentation algorithm initially assigns each sample to one or more sources, or 'catchment basins' in the
     * terminology of the watershed transform. Samples assigned to multiple sources are interpreted as follows: if water
     * was poured into the (inverted) surface up to the level of that sample, the labels indicate which sources would be
     * filled. For the current application, we purge samples assigned to multiple sources and extract only those with
     * unique assignments.
     *
     * @param samplesRaw
     *            Array of all samples (AC-packed)
     * @param alLength
     *            Length of samples window in AL direction
     * @param acLength
     *            Length of samples window in AC direction
     * @param bkg
     *            Background level
     * @param bkgError
     *            Uncertainty (standard deviation) on the background level
     * @return Array containing all the Sources detected.
     */
    private static List<Source> getWatershedSegmentation(float[] samplesRaw, int alLength, int acLength,
            int alPixPerSample, int acPixPerSample, double bkg, double bkgError) {

        // Select appropriate pixel neighbourhood based on samples geometry
        Direction[] neighbourhood = null;
        if (alLength == 1) {
            // 1D windows with no AL extent (e.g. 2D windows that have been marginalised)
            neighbourhood = Direction.NORTH_SOUTH;
        } else if (acLength == 1) {
            // 1D windows with no AC extent (e.g. all window class 1 & 2 transits)
            neighbourhood = Direction.EAST_WEST;
        } else {
            // 2D windows (e.g. class 0 transits, and all SM)
            neighbourhood = Direction.EIGHT_NEIGHBOURS;
        }

        // Create an array and List of Samples. The array is used to get a sample for a given coordinate, and
        // the list is used so that we can process the samples in intensity order
        final Sample[] samplesArray = new Sample[acLength * alLength];
        final List<Sample> sortedSamples = new LinkedList<>();

        // List records the source labels assigned to each sample
        final LabelsList[] labelsArray = new LabelsList[acLength * alLength];

        for (int al = 0; al < alLength; al++) {
            for (int ac = 0; ac < acLength; ac++) {
                // Index of corresponding sample in 1D packed array
                final int i = al * acLength + ac;
                samplesArray[i] = new Sample(al, ac, samplesRaw[i]);
                sortedSamples.add(samplesArray[i]);
                labelsArray[i] = new LabelsList();
            }
        }

        // Sort the collection into order of decreasing intensity
        Collections.sort(sortedSamples);

        // Samples above this level will be considered source
        final double detectionThreshold = bkg + SourceDetector.SOURCE_DETECTION_THRESHOLD_SIGMAS * bkgError;

        // Current source label; incremented each time a new source is found
        int currentLabel = 0;

        // Process samples in decreasing order of intensity
        for (final Sample sample : sortedSamples) {

            // Index of corresponding sample in 1D packed array
            final int i = sample.getAl() * acLength + sample.getAc();

            // Is sample not already assigned to a source?
            if (sample.getLevel() > detectionThreshold && labelsArray[i].isEmpty()) {

                // Found the start of a new source. The rest of this loop expands the source region
                // and labels all connected samples as part of the source.
                final LinkedList<Sample> sourceSamples = new LinkedList<>();
                sourceSamples.add(sample);
                labelsArray[i].add(currentLabel);

                // Now initiate connected region search.
                while (!sourceSamples.isEmpty()) {

                    // Remove Sample from front of queue
                    final Sample sampleToSearch = sourceSamples.poll();

                    // Grow the region by searching among the neighbouring pixels for samples not
                    // already labelled as part of this region (though they could be part of another
                    // region as basins can overlap).
                    final List<Sample> neighbours = getConnectedNeighbours(sampleToSearch, currentLabel,
                            samplesArray, labelsArray, alLength, acLength, neighbourhood);

                    // Sort neighbours into decreasing order of intensity
                    Collections.sort(neighbours);

                    // Check each neighbour in turn to determine if it's part of this source
                    for (final Sample neighbour : neighbours) {

                        // Index of corresponding sample in 1D packed array
                        final int j = neighbour.getAl() * acLength + neighbour.getAc();

                        if (neighbour.getLevel() <= detectionThreshold) {
                            // The remaining neighbour samples will all be lower than this;
                            // can skip the rest of the list
                            break;
                        }

                        // Intensity difference between current sample and this neighbour
                        final double diff = neighbour.getLevel() - sampleToSearch.getLevel();

                        if (diff <= 0) {
                            // Intensity level drops from current sample to neighbour:
                            // the neighbour is part of this source.
                            sourceSamples.add(neighbour);
                            labelsArray[j].add(currentLabel);
                        } else {
                            // Intensity level rises with new neighbour - is the rise above
                            // the noise tolerance level? Background-subtracted source
                            // levels for each sample.
                            final double neighbourLevel = neighbour.getLevel() - bkg;
                            final double sampleLevel = sampleToSearch.getLevel() - bkg;

                            // Variance on 'diff', assuming Poisson statistics
                            final double oneSigma = Math.sqrt(neighbourLevel + sampleLevel);

                            if (diff < CONNECTIVITY_THRESHOLD_SIGMAS * oneSigma) {
                                // Difference is within noise tolerance: the neighbour
                                // is part of this source.
                                sourceSamples.add(neighbour);
                                labelsArray[j].add(currentLabel);
                            }
                        }
                    }

                    // Sort neighbours list into intensity order
                    Collections.sort(sourceSamples);
                }

                // Finished labelling this region; increment label for next region
                currentLabel++;
            }
        }

        // Number of regions detected is indicated in the value of nextLabel.
        // Now extract the samples for these regions into array
        final List<Source> sources = new ArrayList<>();
        for (int i = 0; i < currentLabel; i++) {
            sources.add(new Source(alPixPerSample, acPixPerSample, alLength, acLength));
        }

        // Assign each uniquely-labelled sample to the right source
        for (int al = 0; al < alLength; al++) {
            for (int ac = 0; ac < acLength; ac++) {
                // Index of corresponding sample in 1D packed array
                final int i = al * acLength + ac;

                final Sample sample = samplesArray[i];

                // Is Sample labelled?
                if (labelsArray[i].size() == 1) {

                    final int label = labelsArray[i].get(0);
                    // Add it to the samples list such that the label equals the order of the
                    // source in the source list
                    sources.get(label).getSamples().add(sample);
                }
            }
        }

        return sources;
    }

    /**
     * This source segmentation algorithm follows that described in GAIA-C5-TN-IOA-FVL-144-D:
     *
     * "The samples in the window are sorted into descending order. The image are gradually formed by either assigning
     * an isolated sample to a new source, or assigning a non-isolated sample to an existing source."
     *
     * One drawback of this sample-by-sample approach is that it provides no easy way to merge multiple sources that
     * have been fragmented by noise, so it tends to break diffraction spikes up into many small disconnected sources
     * and detect each as stellar in origin. The watershed algorithm works on a source-by-source basis, expanding each
     * new source until the full area has been detected. This makes it easier to merge sources by not fragmenting them
     * in the first place.
     *
     * @param samplesRaw
     *            Array of all samples (AC-packed)
     * @param alLength
     *            Length of samples window in AL direction
     * @param acLength
     *            Length of samples window in AC direction
     * @param bkg
     *            Background level
     * @param bkgError
     *            Uncertainty (standard deviation) on the background level
     * @return List containing all the Sources detected.
     */
    @SuppressWarnings("unused")
    private static List<Source> getFvlSegmentation(double[] samplesRaw, int alLength, int acLength,
            double bkg, double bkgError) {

        // Create an array and List of Samples. The array is used to get a sample for a given coordinate, and
        // the list is used so that we can process the samples in intensity order
        final Sample[] samplesArray = new Sample[acLength * alLength];
        final List<Sample> sortedSamples = new LinkedList<>();

        // List records the source labels assigned to each sample
        final LabelsList[] labelsArray = new LabelsList[acLength * alLength];

        for (int al = 0; al < alLength; al++) {
            for (int ac = 0; ac < acLength; ac++) {
                // Index of corresponding sample in 1D packed array
                final int i = al * acLength + ac;
                samplesArray[i] = new Sample(al, ac, samplesRaw[i]);
                sortedSamples.add(samplesArray[i]);
                labelsArray[i] = new LabelsList();
            }
        }

        // Sort the collection into order of decreasing intensity
        Collections.sort(sortedSamples);

        // Samples above this level will be considered source
        final double detectionThreshold = bkg + SourceDetector.SOURCE_DETECTION_THRESHOLD_SIGMAS * bkgError;

        // Select appropriate pixel neighbourhood based on samples geometry
        Direction[] neighbourhood = null;
        if (alLength == 1) {
            // 1D windows with no AL extent (e.g. 2D windows that have been marginalised)
            neighbourhood = Direction.NORTH_SOUTH;
        } else if (acLength == 1) {
            // 1D windows with no AC extent (e.g. all window class 1 & 2 transits)
            neighbourhood = Direction.EAST_WEST;
        } else {
            // 2D windows (e.g. class 0 transits, and all SM)
            neighbourhood = Direction.EIGHT_NEIGHBOURS;
        }

        // Current source label
        int currentLabel = 0;

        // List of all detected Sources
        final List<Source> sources = new LinkedList<>();

        // Process samples in decreasing order of intensity
        for (final Sample sample : sortedSamples) {

            // Is sample above the source detection threshold?
            if (sample.getLevel() > detectionThreshold) {

                // Index of sample in 1D array
                final int sampleIndex = sample.getAl() * acLength + sample.getAc();

                // List of existing Source labels to which this samples is connected
                final List<Integer> connectedSources = new LinkedList<>();

                // Loop over neighbouring pixels
                for (final Direction dir : neighbourhood) {
                    // Coordinate of this neighbour
                    final int al_i = sample.getAl() + dir.dal;
                    final int ac_i = sample.getAc() + dir.dac;
                    // Is this neighbour outside of the window area?
                    if (al_i < 0 || al_i >= alLength || ac_i < 0 || ac_i >= acLength) {
                        continue;
                    }

                    // Index of neighbour in 1D array
                    final int neighbourIndex = al_i * acLength + ac_i;

                    // Is neighbour already labelled as part of a source?
                    // (no need to explicitly check that the neighbour is above the threshold:
                    // if it isn't then it won't have been labelled)
                    if (!labelsArray[neighbourIndex].isEmpty()) {

                        // Retrieve the first element of the labels array as the label of this
                        // neighbouring sample. In the FvL algorithm, each sample is only
                        // connected to one source so there is only ever a single label for
                        // labelled sources.
                        final Integer label = labelsArray[neighbourIndex].get(0);

                        // Only want unique labels - if several samples have the same label
                        // (assigned to the same source) then we just want to log a single
                        // label value.
                        if (!connectedSources.contains(label)) {
                            connectedSources.add(label);
                        }
                    }
                }

                if (connectedSources.isEmpty()) {
                    // New source!
                    labelsArray[sampleIndex].add(currentLabel++);
                    final Source newSource = new Source();
                    newSource.getSamples().add(sample);
                    sources.add(newSource);
                } else if (connectedSources.size() == 1) {
                    // Sample is part of an existing source!
                    labelsArray[sampleIndex].add(connectedSources.get(0));
                    sources.get(connectedSources.get(0)).getSamples().add(sample);
                } else {
                    // Sample is at the boundary of two existing sources: do not add to either.
                }
            }
        }

        return sources;
    }

    /**
     * This method computes, for the given pixel, the set of neighbouring pixels that are above the intensity threshold
     * and not labelled with the current label. This is a crucial step in the region-growing stage of the image
     * segmentation algorithm.
     *
     * @param sample
     *            Central sample
     * @param currentLabel
     *            The current label; only neighbours that don't have this label are returned
     * @param samples
     *            Full set of all samples
     * @param labels
     *            List of source labels applied to each sample
     * @param alLength
     *            AL length of 2D window
     * @param acLength
     *            AC length of 2D window
     * @param neighbourhood
     *            Defines the pixel neighbourhood
     * @return List of Samples that are part of the neighbourhood of the central pixel, and that are not labelled with
     *         the given label
     */
    private static LinkedList<Sample> getConnectedNeighbours(Sample sample, Integer currentLabel, Sample[] samples,
            LabelsList[] labels, int alLength, int acLength, Direction[] neighbourhood) {

        // List of unlabelled neighbours
        final LinkedList<Sample> neighbours = new LinkedList<>();

        // Coordinate of this sample
        final int al = sample.getAl();
        final int ac = sample.getAc();

        // Loop over neighbouring pixels
        for (final Direction dir : neighbourhood) {
            // Coordinate of this neighbour
            final int al_i = al + dir.dal;
            final int ac_i = ac + dir.dac;

            // Is this neighbour outside of the window area?
            if (al_i < 0 || al_i >= alLength || ac_i < 0 || ac_i >= acLength) {
                continue;
            }

            // Check if it's already labelled as part of this source
            if (labels[al_i * acLength + ac_i].contains(currentLabel)) {
                continue;
            }

            // Retrieve this sample
            final Sample neighbour = samples[al_i * acLength + ac_i];

            // Found a neighbouring sample that is above the threshold and is not already labelled
            neighbours.add(neighbour);
        }

        return neighbours;
    }

    /**
     * Computes the peak flux, integrated flux and peak-to-neighbour flux ratio, and sets the relevant fields in the
     * Source.
     *
     * @param source
     *            The Source to examine
     * @param samples
     *            The array of raw samples
     * @param alLength
     *            The number of samples in the AL direction
     * @param acLength
     *            The number of samples in the AC direction
     * @param bkg
     *            The background level and error
     */
    private static void computeSourceFluxStatistics(Source source, float[] samples, int alLength,
            int acLength, double[] bkg) {

        // Select appropriate pixel neighbourhood based on samples geometry
        Direction[] neighbourhood = null;
        if (alLength == 1) {
            // 1D windows with no AL extent (e.g. 2D windows that have been marginalised)
            neighbourhood = Direction.NORTH_SOUTH;
        } else if (acLength == 1) {
            // 1D windows with no AC extent (e.g. all window class 1 & 2 transits)
            neighbourhood = Direction.EAST_WEST;
        } else {
            // 2D windows (e.g. class 0 transits, and all SM)
            neighbourhood = Direction.EIGHT_NEIGHBOURS;
        }

        // Integrated flux
        double intFlux = 0.0;

        // Level of the largest sample in the source
        double peakFlux = -Double.MAX_VALUE;
        // Coordinates of the largest sample
        int largestSampleAc = 0;
        int largestSampleAl = 0;

        for (final Sample sample : source.getSamples()) {

            final double bkgSubSample = sample.getLevel() - bkg[0];

            // Compute integrated flux
            intFlux += bkgSubSample;

            // Track the largest sample and it's location
            if (bkgSubSample > peakFlux) {
                peakFlux = bkgSubSample;
                largestSampleAc = sample.getAc();
                largestSampleAl = sample.getAl();
            }
        }
        source.setFlux(intFlux);

        // Compute the ratio between the flux of the largest sample and it's immediate
        // neighbours (which may not be members of the source)

        final FloatList fluxNeighbours = new FloatList();

        // Loop over neighbouring pixels
        for (final Direction dir : neighbourhood) {
            // Coordinate of this neighbour
            final int al_i = largestSampleAl + dir.dal;
            final int ac_i = largestSampleAc + dir.dac;

            // Is this neighbour outside of the window area?
            if (al_i < 0 || al_i >= alLength || ac_i < 0 || ac_i >= acLength) {
                continue;
            }

            // Retrieve this sample. Subtract the background level.
            final double neighbour = samples[al_i * acLength + ac_i] - bkg[0];

            fluxNeighbours.add((float)neighbour);
        }

        // Get the median value from the 50% percentile
        final double medianNeighbourFlux = fluxNeighbours.getPercentile(50.0f);

        // The ratio between the flux of the strongest sample and the avergae of it's immediate neighbours
        final double fluxRatio = peakFlux / medianNeighbourFlux;

        source.setPeakFlux(peakFlux);
        source.setFluxRatio(fluxRatio);
    }

    /**
     * Computes the principal axes, eigenvalues and orientation of the intensity distribution, and sets the relevant
     * fields in the Source.
     *
     * @param source
     *            The Source to examine
     * @param samples
     *            The array of raw samples
     * @param alLength
     *            The number of samples in the AL direction
     * @param acLength
     *            The number of samples in the AC direction
     * @param alPixPerSample
     *            Number of pixels in a sample, in the AL direction. Used to scale sample coordinates to pixel
     *            coordinates to avoid unequal binning in AC & AL (e.g. in AF1) making sources look more elongated and
     *            spike-like than they really are.
     * @param acPixPerSample
     *            Number of pixels in a sample, in the AC direction. Used to scale sample coordinates to pixel
     *            coordinates to avoid unequal binning in AC & AL (e.g. in AF1) making sources look more elongated and
     *            spike-like than they really are.
     * @param bkg
     *            The background level and error
     */
    private static void computeSourceShapeStatistics(Source source, float[] samples, int alLength,
            int acLength,  int alPixPerSample, int acPixPerSample, double[] bkg) {

        // Compute the principal axes and orientation of the intensity distribution
        final double[] eigs = new double[] { Double.NaN, Double.NaN };
        double orientation = Double.NaN;
        
        if(source.getSamples().isEmpty()) {
            source.setEigenvalues(eigs);
            source.setOrientation(orientation);
            return;
        }
        
        // Samples to use in computing the shape statistics: the peak sample and the 8 nearest neighbours (regardless
        // of whether they were marked as part of the source - this is necessary for cosmic rays), then any others
        // samples marked as part of the source.
        
        // Select appropriate pixel neighbourhood based on samples geometry
        Direction[] neighbourhood = null;
        if (alLength == 1) {
            // 1D windows with no AL extent (e.g. 2D windows that have been marginalised)
            neighbourhood = Direction.NORTH_SOUTH;
        } else if (acLength == 1) {
            // 1D windows with no AC extent (e.g. all window class 1 & 2 transits)
            neighbourhood = Direction.EAST_WEST;
        } else {
            // 2D windows (e.g. class 0 transits, and all SM)
            neighbourhood = Direction.EIGHT_NEIGHBOURS;
        }
        
        List<Sample> samplesToUse = new LinkedList<Sample>();
        Sample peakSample = source.getSamples().get(0);
        for (final Sample sample : source.getSamples()) {
        	if(sample.getLevel() > peakSample.getLevel()) {
        		peakSample = sample;
        	}
        }
        
        // Loop over neighbouring pixels
        for (final Direction dir : neighbourhood) {
            // Coordinate of this sample
            final int al_i = peakSample.getAl() + dir.dal;
            final int ac_i = peakSample.getAc() + dir.dac;

            // Is this sample outside of the window area?
            if (al_i < 0 || al_i >= alLength || ac_i < 0 || ac_i >= acLength) {
                continue;
            }
            
            // Flux level for this sample:
            double flux = samples[al_i * acLength + ac_i];
            Sample sample = new Sample(al_i, ac_i, flux);
            samplesToUse.add(sample);
        }
        
        // Add remaining samples that are part of this source but not yet included
        for (final Sample sample : source.getSamples()) {
        	if(!samplesToUse.contains(sample)) {
        		samplesToUse.add(sample);
        	}
        }
        
        // Compute the centre-of-flux [pix]
        double acFluxCentre = 0.0;
        double alFluxCentre = 0.0;
        double sumFlux = 0.0;
        
        for (final Sample sample : samplesToUse) {
            acFluxCentre += sample.getAc() * acPixPerSample * (sample.getLevel() - bkg[0]);
            alFluxCentre += sample.getAl() * alPixPerSample * (sample.getLevel() - bkg[0]);
            sumFlux += sample.getLevel() - bkg[0];
        }
        acFluxCentre /= sumFlux;
        alFluxCentre /= sumFlux;

        // Compute the flux-weighted sample position dispersion matrix [pix], as A =
        // [a b]
        // [b c]
        double a = 0.0;
        double b = 0.0;
        double c = 0.0;
        
        for (final Sample sample : samplesToUse) {	
        	
            final double weight = (sample.getLevel() - bkg[0]) / sumFlux;
            a += (sample.getAc() * acPixPerSample - acFluxCentre) *
                    (sample.getAc() * acPixPerSample - acFluxCentre) * weight;
            b += (sample.getAc() * acPixPerSample - acFluxCentre) *
                    (sample.getAl() * alPixPerSample - alFluxCentre) * weight;
            c += (sample.getAl() * alPixPerSample - alFluxCentre) *
                    (sample.getAl() * alPixPerSample - alFluxCentre) * weight;
        }
        
        // Compute the eigenvalues: direct solution for 2x2 matrix
        final double tr = a + c;
        final double det = a * c - b * b;
        double disc = tr * tr / 4.0 - det;
        if (disc < 0.0) {
            // Eigenvalues are complex; note that the dispersion matrix is
            // real-symmetric so eigenvalues (should be) always real, however we explicitly
            // test this here in order to avoid unanticipated exceptions.
            source.setEigenvalues(eigs);
            source.setOrientation(orientation);
            System.out.println("Eigenvalues complex");
            return;
        }
        disc = Math.sqrt(disc);
        // The eigenvalues
        final double l1 = tr / 2.0 + disc;
        final double l2 = tr / 2.0 - disc;

        // Update the eigenvalues for the source
        eigs[0] = l1;
        eigs[1] = l2;

        // Both eigenvalues should be positive, as we're working with intensity maxima. Usually if
        // either is negative then the source is a perfect straight line.
        // NOTE: disabled this test, because the flux-weighted sample dispersion matrix can throw out
        // negative eigenvalues for reasonable sources (cosmics with few samples).
//        if (l1 < 0.0 || l2 < 0.0) {
//            source.setEigenvalues(eigs);
//            source.setOrientation(orientation);
//            return;
//        }

        // We have detected a spike. Assign an orientation wrt the AL direction by analysing the
        // eigenvector corresponding to the largest eigenvalue.
        if (b == 0.0) {
            // Special case: principal axes align with AC/AL directions. Likely that one eigenvalue
            // is zero.
            if (a > c) {
                orientation = Math.PI / 2.0;
            } else {
                orientation = 0.0;
            }
        } else {
            // General case: principal axis does not align with either AC or AL direction
            final double lmax = Math.max(l1, l2);

            // AL and AC components of (normalised) eigenvector corresponding to largest eigenvalue.
            // We take the absolute value in order to reflect the eigenvector into the first
            // quadrant so as to ease the algebra for computing the angle between the vector at
            // the axes.
            final double v_al = Math.abs(1.0 / Math.sqrt(b * b / ((a - lmax) * (a - lmax)) + 1.0));
            final double v_ac = Math.abs(Math.sqrt(1 - v_al));

            // Get the angle between the spike and the AL direction
            orientation = Math.atan(v_ac / v_al);
        }
            
        source.setEigenvalues(eigs);
        source.setOrientation(orientation);
    }

}
