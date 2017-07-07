package dm;

import java.io.Serializable;

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
	 * Observation time for this window
	 * TODO: decide on the units for this
	 */
	public final double obsTime;

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
	 * @param obsTime
	 * 	Observation time for this window
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
	public Window(byte fov, byte row, byte strip, short acWinCoord, byte gateNum, long transitId, double obsTime,
			int alSamples, int acSamples, int alSampleSize, int acSampleSize, double intTime, float[] samples) {
		this.fov = fov;
		this.row = row;
		this.strip = strip;
		this.acWinCoord = acWinCoord;
		this.gate = gateNum;
		this.transitId = transitId;
		this.obsTime = obsTime;
		this.alSamples = alSamples;
		this.acSamples = acSamples;
		this.alSampleSize = alSampleSize;
		this.acSampleSize = acSampleSize;
		this.intTime = intTime;
		this.samples = samples;
	}
	
}