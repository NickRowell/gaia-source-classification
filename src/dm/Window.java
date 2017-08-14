package dm;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dm.Source.Type;

/**
 * Class represents a single 2D window measured by Gaia. It encapsulates all the fields required by the
 * source classification project.
 *
 * @author nrowell
 * @version $Id$
 */
public class Window implements Serializable {
	
	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = -7909026931832439667L;

	/**
	 * The field-of-view number (0 or 1)
	 */
	public final byte fov;
	
	/**
	 * The CCD row number (1->7)
	 */
	public final byte row;
	
	/**
	 * The CCD strip number (4->12 for AF1-9 in Astrometric Field)
	 */
	public final byte strip;
	
	/**
	 * Position of the window in the AC direction on the CCD on which it was detected (0->2000)
	 */
	public final short acWinCoord;
	
	/**
	 * The CCD gate used by this observation (0->12)
	 */
	public final byte gate;
	
	/**
	 * The transit ID for this observation
	 */
	public final long transitId;
	
	/**
	 * Observation time for this window [rev]. This is the onboard mission time (OBMT) measured in
	 * revolutions (of the satellite) since some relatively arbitrary zeropoint. For converting this
	 * to a meaningful time in e.g. UTC the following zeropoint offset can be used:
	 * 
	 * OBMT 3500.0 [rev] = 2016-03-21T20:13:20.716226974 [UTC]
	 */
	public final double obmtRev;

	/**
	 * Window size (number of samples) in the AL direction
	 */
	public final int alSamples;
	
	/**
	 * Window size (number of samples) in the AC direction
	 */
	public final int acSamples;
	
	/**
	 * Sample size (number of pixels per sample) in the AL direction
	 */
	public final int alSampleSize;
	
	/**
	 * Sample size (number of pixels per sample) in the AC direction
	 */
	public final int acSampleSize;
	
	/**
	 * Integration time for this window [s]
	 */
	public final double intTime;
	
	/**
	 * The raw samples for this window [e/pixel]
	 */
	public final float[] samples;
	
	/**
	 * List containing any {@link Source}s detected in the {@link Window}.
	 */
	public final List<Source> sources;
	
	/**
	 * Main constructor for the {@link Window}.
	 * @param fov
	 * 	The field-of-view number (0 or 1)
	 * @param row
	 * 	The CCD row number (1->7)
	 * @param strip
	 * 	The CCD strip number (4->12 for AF1-9 in Astrometric Field)
	 * @param acWinCoord
	 * 	Position of the window in the AC direction on the CCD on which it was detected (0->2000)
	 * @param gateNum
	 * 	The CCD gate used by this observation (0->12)
	 * @param transitId
	 * 	The transit ID for this observation
	 * @param obmtRev
	 * 	The onboard mission time of observation for this window, in revolutions [revs]
	 * @param alSamples
	 * 	Window size (number of samples) in the AL direction
	 * @param acSamples
	 * 	Window size (number of samples) in the AC direction
	 * @param alSampleSize
	 * 	Sample size (number of pixels per sample) in the AL direction
	 * @param acSampleSize
	 * 	Sample size (number of pixels per sample) in the AC direction
	 * @param intTime
	 * 	Integration time for this window [s]
	 * @param samples
	 * 	The raw samples for this window [e/pixel]
	 */
	public Window(byte fov, byte row, byte strip, short acWinCoord, byte gateNum, long transitId, double obmtRev,
			int alSamples, int acSamples, int alSampleSize, int acSampleSize, double intTime, float[] samples) {
		this.fov = fov;
		this.row = row;
		this.strip = strip;
		this.acWinCoord = acWinCoord;
		this.gate = gateNum;
		this.transitId = transitId;
		this.obmtRev = obmtRev;
		this.alSamples = alSamples;
		this.acSamples = acSamples;
		this.alSampleSize = alSampleSize;
		this.acSampleSize = acSampleSize;
		this.intTime = intTime;
		this.samples = samples;
		this.sources = new LinkedList<>();
	}

    /**
     * Encodes this {@link Window} as an array of bytes suitable for writing to
     * a binary file.
     * @return
     * 	An array of bytes suitable for writing to a binary file.
     */
    public byte[] toByteArray() {
    	
    	// Create a ByteBuffer of the right capacity.
    	// 18*12 samples of 4 bytes each
    	// Number of each type of source; 8 types of one byte each
    	// OBMT of 8 bytes
    	ByteBuffer b = ByteBuffer.allocate(18*12*4 + Type.values().length + 8);
    	
    	// Count the numbers of each {@link Source} type
    	Map<Type, byte[]> sourceCounts = new EnumMap<>(Type.class);
    	for(Type type : Type.values()) {
    		sourceCounts.put(type, new byte[1]);
    	}
    	for(Source source : sources) {
    		sourceCounts.get(source.getType())[0]++;
    	}
    	
    	// Write the 216 samples; 4 bytes each
    	for(float sample : samples) {
    		b.putFloat(sample);
    	}
    	// Write the number of each type of {@link Source}; 1 byte each
    	for(Type type : Type.values()) {
    		b.put(sourceCounts.get(type)[0]);
    	}
    	// Write the OBMT; 8 bytes
    	b.putDouble(obmtRev);
    	
    	// TODO: add other fields as necessary
    	
    	return b.array();
    }
}