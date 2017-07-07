package util;

/**
 * Utilities relating to the Gaia satellite.
 *
 *
 * @author nrowell
 * @version $Id$
 */
public class GaiaUtil {
	
	/**
	 * Array of Strings that associate the CCD strip index [1-12] with a human-readable name for the strip.
	 */
	public static final String[] stripNames = {"", "BAM/WFS", "SM1", "SM2", "AF1", "AF2", "AF3", "AF4", "AF5", "AF6", "AF7", "AF8", "AF9"};

	/**
	 * Array of Strings that associate the CCD gate index [0-12] with a human-readable name for the gate.
	 */
	public static final String[] gateNames = {"NOGATE", "GATE1", "GATE2", "GATE3", "GATE4", "GATE5", "GATE6", 
			"GATE7", "GATE8", "GATE9", "GATE10", "GATE11", "GATE12"};

}
