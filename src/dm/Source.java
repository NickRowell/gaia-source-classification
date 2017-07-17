package dm;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class used to represent a single Source detected in a window.
 *
 * @author nrowell
 * @version $Id: Source.java 471308 2015-12-14 12:09:36Z nrowell $
 */
public class Source implements Serializable {
	
    /**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 1087540357779804293L;

	/**
     * The Type enum for Source classification.
     */
    public static enum Type {
        /**
         * Stellar source.
         */
        STELLAR("A normal stellar source"),
        /**
         * Cosmic ray.
         */
        COSMIC("A cosmic ray"),
        /**
         * Diffraction spike in the AL direction.
         */
        SPIKE_AL("A diffraction spike in the AL direction"),
        /**
         * Diffraction spike in the AC direction.
         */
        SPIKE_AC("A diffraction spike in the AC direction"),
        /**
         * Diffraction spike in a diagonal direction.
         */
        SPIKE_DIAGONAL("A diffraction spike in a diagonal direction"),
        /**
         * Source that could not be classified as a known type.
         */
        UNKNOWN("An unknown source type");
        
    	/**
    	 * A short description of each {@link Type}
    	 */
    	String description;
    	
        Type(String description) {
        	this.description = description;
        }
        
        /**
         * Get a short description of the source type classification.
         * @return
         * 	A short description of the source type classification.
         */
        public String getDescription() {
        	return description;
        }
    };
	
    /**
     * The List of Samples designated to this source.
     */
    private final List<Sample> samples;

    /**
     * The AL sample size [pixels]
     */
    private int alSampleSize;

    /**
     * The AC sample size [pixels]
     */
    private int acSampleSize;

    /**
     * The AL window size [samples]
     */
    private int alWinSize;

    /**
     * The AC window size [samples]
     */
    private int acWinSize;

    /**
     * The Type classification for this Source.
     */
    private Type type;

    /**
	 * Observation time for this source [rev]. This is the onboard mission time (OBMT) measured in
	 * revolutions (of the satellite) since some relatively arbitrary zeropoint. For converting this
	 * to a meaningful time in e.g. UTC the following zeropoint offset can be used:
	 * 
	 * OBMT 3500.0 [rev] = 2016-03-21T20:13:20.716226974 [UTC]
	 */
	private double obmtRev;
    
    /**
     * The total integrated (background-subtracted) samples [e-]
     */
    private double flux;

    /**
     * Peak (background-subtracted) sample [e-]
     */
    private double peakFlux;

    /**
     * Ratio of peak (background-subtracted) sample to average of (background-subtracted) neighbours.
     */
    private double fluxRatio;

    /**
     * Eigenvalues of the flux-weighted sample dispersion matrix [pixels].
     */
    private double[] eigs;

    /**
     * Orientation of the principal axis of the flux weighted sample dispersion matrix, expressed as the angle between
     * the principal axis and the AL direction [0:pi/2.0] [radians]
     */
    private double orientation;

    /**
     * Default constructor.
     */
    public Source() {
        samples = new LinkedList<>();
        type = Type.UNKNOWN;
        eigs = null;
    }

    /**
     * Constructor setting the window and sample sizes.
     *
     * @param alSampleSize
     *            The AL sample size [pixels]
     * @param acSampleSize
     *            The AC sample size [pixels]
     * @param alWinSize
     *            The AL window size [samples]
     * @param acWinSize
     *            The AC window size [samples]
     */
    public Source(int alSampleSize, int acSampleSize, int alWinSize, int acWinSize) {
        samples = new LinkedList<>();
        type = Type.UNKNOWN;
        eigs = null;
        this.alSampleSize = alSampleSize;
        this.acSampleSize = acSampleSize;
        this.alWinSize = alWinSize;
        this.acWinSize = acWinSize;
    }

    /**
     * Gets the internal List of Samples corresponding to this Source.
     *
     * @return The internal List of Samples corresponding to this Source.
     */
    public List<Sample> getSamples() {
        return samples;
    }

    /**
     * Get the AL sample size [pixels]
     *
     * @return The AL sample size [pixels]
     */
    public int getAlSampleSize() {
        return this.alSampleSize;
    }

    /**
     * Get the AC sample size [pixels]
     *
     * @return The AC sample size [pixels]
     */
    public int getAcSampleSize() {
        return this.acSampleSize;
    }

    /**
     * Get the AL window size [samples]
     *
     * @return The AL window size [samples]
     */
    public int getAlWinSize() {
        return this.alWinSize;
    }

    /**
     * Get the AC window size [samples]
     *
     * @return The AC window size [samples]
     */
    public int getAcWinSize() {
        return this.acWinSize;
    }

    /**
     * Get the dimensionality of the Source window.
     *
     * @return True if the Source is extracted from a 1D window; false if 2D.
     */
    public boolean getIs1D() {
        return alWinSize == 1 || acWinSize == 1;
    }

    /**
     * Get the total integrated (background-subtracted) source flux.
     *
     * @return The total integrated (background-subtracted) source flux [e-]
     */
    public double getFlux() {
        return flux;
    }

    /**
     * Set the total integrated (background-subtracted) source flux.
     *
     * @param flux
     *            The total integrated (background-subtracted) source flux [e-]
     */
    public void setFlux(double flux) {
        this.flux = flux;
    }

    /**
     * Get the observation time for this source [rev]. This is the onboard mission time (OBMT) measured in
	 * revolutions (of the satellite) since some relatively arbitrary zeropoint. For converting this
	 * to a meaningful time in e.g. UTC the following zeropoint offset can be used:
	 * 
	 * OBMT 3500.0 [rev] = 2016-03-21T20:13:20.716226974 [UTC]
	 * 
	 * @return
	 * 	The observation time for this source [rev].
     */
    public double getObmtRev() {
        return obmtRev;
    }

    /**
     * Set the observation time for this source [rev].
     * 
     * @param obmtRev
	 * 	The observation time for this source [rev].
     */
    public void setObmtRev(double obmtRev) {
        this.obmtRev = obmtRev;
    }
    
    /**
     * Get the peak (background-subtracted) sample [e-].
     *
     * @return The peak (background-subtracted) sample [e-].
     */
    public double getPeakFlux() {
        return peakFlux;
    }

    /**
     * Set the peak (background-subtracted) sample [e-].
     *
     * @param peakFlux
     *            The peak (background-subtracted) sample [e-].
     */
    public void setPeakFlux(double peakFlux) {
        this.peakFlux = peakFlux;
    }

    /**
     * Get the ratio of the largest sample in the source to it's immediate neighbours.
     *
     * @return The ratio of the largest sample in the source to it's immediate neighbours.
     */
    public double getFluxRatio() {
        return fluxRatio;
    }

    /**
     * Set the ratio of the largest sample in the source to it's immediate neighbours.
     *
     * @param fluxRatio
     *            The ratio of the largest sample in the source to it's immediate neighbours.
     */
    public void setFluxRatio(double fluxRatio) {
        this.fluxRatio = fluxRatio;
    }

    /**
     * Get the Eigenvalues of the flux-weighted sample dispersion matrix [pixels].
     *
     * @return Eigenvalues of the flux-weighted sample dispersion matrix [pixels].
     */
    public double[] getEigenvalues() {
        return eigs;
    }

    /**
     * Set the Eigenvalues of the flux-weighted sample dispersion matrix [pixels].
     *
     * @param evs
     *            Eigenvalues of the flux-weighted sample dispersion matrix [pixels].
     */
    public void setEigenvalues(double[] evs) {
        this.eigs = evs;
    }

    /**
     * Get the orientation of the principal axis of the flux weighted sample dispersion matrix [degrees]
     *
     * @return Orientation of the principal axis of the flux weighted sample dispersion matrix [degrees]
     */
    public double getOrientation() {
        return orientation;
    }

    /**
     * Set the orientation of the principal axis of the flux weighted sample dispersion matrix [degrees]
     *
     * @param orientation
     *            Orientation of the principal axis of the flux weighted sample dispersion matrix [degrees]
     */
    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    /**
     * Gets the Type classification for this Source.
     *
     * @return The Type classification for this Source.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the Type classification for this Source.
     *
     * @param type
     *            The Type classification for the Source.
     */
    public void setType(Type type) {
        this.type = type;
    }
    
    /**
     * Override the equals method for {@link Source}.
     */
    @Override
    public boolean equals(Object obj) {
    	 if (!(obj instanceof Source))
             return false;
         if (obj == this)
             return true;

         Source source = (Source) obj;
         
         // Compare all fields
         if(this.alSampleSize != source.alSampleSize) {
        	 return false;
         }
         if(this.acSampleSize != source.acSampleSize) {
        	 return false;
         }
         if(this.alWinSize != source.alWinSize) {
        	 return false;
         }
         if(this.acWinSize != source.acWinSize) {
        	 return false;
         }
         if(this.type != source.type) {
        	 return false;
         }
         if(this.flux != source.flux) {
        	 return false;
         }
         if(this.peakFlux != source.peakFlux) {
        	 return false;
         }
         if(this.fluxRatio != source.fluxRatio) {
        	 return false;
         }
         if(!new Double(this.orientation).equals(new Double(source.orientation))) {
        	 return false;
         }
         if(!Arrays.equals(this.eigs, source.eigs)) {
        	 return false;
         }
         // Compare individual samples
         if(this.samples.size() != source.samples.size()) {
        	 return false;
         }
         for(Sample sample : this.samples) {
        	 if(!source.samples.contains(sample)) {
        		 // A sample is missing
        		 return false;
        	 }
         }
         
         return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
    	
    	StringBuilder str = new StringBuilder();
    	str.append(flux + "\t");
    	str.append(peakFlux + "\t");
    	str.append(fluxRatio + "\t");
    	str.append(eigs[0] + "\t" + eigs[1] + "\t");
    	str.append(orientation + "\t");
    	str.append(type);
    	return str.toString();
    }
    
    /**
     * Encodes this {@link Source} as an array of bytes suitable for writing to
     * a binary file.
     * @return
     * 	An array of bytes suitable for writing to a binary file.
     */
    public byte[] toByteArray() {
    	
    	// Create a ByteBuffer of the right capacity
    	ByteBuffer b = ByteBuffer.allocate(57);
    	
    	// TODO: add other fields as necessary
    	b.putDouble(flux);                  // 8 bytes
    	b.putDouble(peakFlux);              // 8 bytes
    	b.putDouble(fluxRatio);             // 8 bytes
    	b.putDouble(eigs[0]);               // 8 bytes
    	b.putDouble(eigs[1]);               // 8 bytes
    	b.putDouble(orientation);           // 8 bytes
    	b.put((byte)type.ordinal());        // 1 byte
    	b.putDouble(obmtRev);			    // 8 bytes
    	
    	return b.array();
    }
    
}