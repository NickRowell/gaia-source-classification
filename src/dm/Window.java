package dm;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

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
     * Encodes the {@link Window} as an array of bytes. Note that the {@link Window#sources}
     * field is not encoded.
     * 
     * @return
     * 	An array of bytes suitable for writing to a binary file.
     */
    public static byte[] toByteArray(Window win) {
    	
    	// Create a ByteBuffer of the right capacity.
    	ByteBuffer b = ByteBuffer.allocate(46 + 4*win.samples.length);
    	    	
    	// Write the field-of-view number; 1 byte
    	b.put(win.fov);

    	// Write the row number; 1 byte
    	b.put(win.row);
    	
    	// Write the strip number; 1 byte
    	b.put(win.strip);
    	
    	// Write the AC window coordinate [pixels]; 2 byte
    	b.putShort(win.acWinCoord);
    	
    	// Write the gate number; 1 byte
    	b.put(win.gate);
    	
    	// Write the transit ID; 8 byte
    	b.putLong(win.transitId);
    	
    	// Write the OBMT; 8 bytes
    	b.putDouble(win.obmtRev);
    	
    	// Write the AL samples; 4 bytes
    	b.putInt(win.alSamples);

    	// Write the AC samples; 4 bytes
    	b.putInt(win.acSamples);
    	
    	// Write the AL sample size; 4 bytes
    	b.putInt(win.alSampleSize);

    	// Write the AC sample size; 4 bytes
    	b.putInt(win.acSampleSize);

    	// Write the integration time [secs]; 8 bytes
    	b.putDouble(win.intTime);
    	
    	// Write the samples; 4 bytes each
    	for(float sample : win.samples) {
    		b.putFloat(sample);
    	}
    	
    	// Count the numbers of each {@link Source} type
//    	Map<Type, byte[]> sourceCounts = new EnumMap<>(Type.class);
//    	for(Type type : Type.values()) {
//    		sourceCounts.put(type, new byte[1]);
//    	}
//    	for(Source source : sources) {
//    		sourceCounts.get(source.getType())[0]++;
//    	}
    	// Write the number of each type of {@link Source}; 1 byte each
//    	for(Type type : Type.values()) {
//    		b.put(sourceCounts.get(type)[0]);
//    	}    	
    	
    	return b.array();
    }
    
    /**
     * Decodes a {@link Window} from an array of bytes. Note that the {@link Window#sources}
     * field is not decoded.
     * 
     * @param array
     * 	The array of bytes.
     * @param p
     * 	Single-element int array storing the index of the element to start reading from in the byte array.
     * This is advanced to the end of the current {@link Window} so that on exit it stores the index to start reading
     * the next {@link Window} from.
     * @return
     * 	A {@link Window} decoded from the byte array.
     */
    public static Window fromByteArray(byte[] array, int[] p) {
    	
    	byte fov = array[p[0] + 0];
    	byte row = array[p[0] + 1];
    	byte strip = array[p[0] + 2];
    	short acWinCoord = getShort(array, p[0] + 3);
    	byte gateNum = array[p[0] + 5];
    	long trId = getLong(array, p[0] + 6);
    	double obmtRev = getDouble(array, p[0] + 14);
    	int alSamples = getInt(array, p[0] + 22);
    	int acSamples = getInt(array, p[0] + 26);
    	int alSampleSize = getInt(array, p[0] + 30);
    	int acSampleSize = getInt(array, p[0] + 34);
    	double intTime = getDouble(array, p[0] + 38);
    	float[] samples = new float[alSamples * acSamples];
    	for(int i=0; i<samples.length; i++) {
    		samples[i] = getFloat(array, p[0] + 46 + (4*i));
    	}
    	
    	// Move on pointer
    	p[0] += 46 + 4 * samples.length;
    	
    	return new Window(fov, row, strip, acWinCoord, gateNum, trId, obmtRev,
    			alSamples, acSamples, alSampleSize, acSampleSize, intTime, samples);
    }
    
    /**
     * Construct a double from 8 bytes stored at the given contiguous position in the array.
     * @param array
     * @param start
     * 	Starting element (inclusive)
     * @return
     * 	A double
     */
    private static double getDouble(byte[] array, int start) {
    	if(array.length - start < 8) {
    		throw new RuntimeException("Cannot retrieve 8 bytes from array of length " + array.length + " with starting point " + start);
    	}
    	byte[] subArray = new byte[8];
    	System.arraycopy(array, start, subArray, 0, 8);
    	return ByteBuffer.wrap(subArray).getDouble();
    }

    /**
     * Construct a float from 4 bytes stored at the given contiguous position in the array.
     * @param array
     * @param start
     * 	Starting element (inclusive)
     * @return
     * 	A float
     */
    private static float getFloat(byte[] array, int start) {
    	if(array.length - start < 4) {
    		throw new RuntimeException("Cannot retrieve 4 bytes from array of length " + array.length + " with starting point " + start);
    	}
    	byte[] subArray = new byte[4];
    	System.arraycopy(array, start, subArray, 0, 4);
    	return ByteBuffer.wrap(subArray).getFloat();
    }
    
    /**
     * Construct an int from 4 bytes stored at the given contiguous position in the array.
     * @param array
     * @param start
     * 	Starting element (inclusive)
     * @return
     * 	An int
     */
    private static int getInt(byte[] array, int start) {
    	if(array.length - start < 4) {
    		throw new RuntimeException("Cannot retrieve 4 bytes from array of length " + array.length + " with starting point " + start);
    	}
    	byte[] subArray = new byte[4];
    	System.arraycopy(array, start, subArray, 0, 4);
    	return ByteBuffer.wrap(subArray).getInt();
    }
    
    /**
     * Construct a long from 8 bytes stored at the given contiguous position in the array.
     * @param array
     * @param start
     * 	Starting element (inclusive)
     * @return
     * 	A long
     */
    private static long getLong(byte[] array, int start) {
    	if(array.length - start < 8) {
    		throw new RuntimeException("Cannot retrieve 8 bytes from array of length " + array.length + " with starting point " + start);
    	}
    	byte[] subArray = new byte[8];
    	System.arraycopy(array, start, subArray, 0, 8);
    	return ByteBuffer.wrap(subArray).getLong();
    }

    /**
     * Construct a short from 2 bytes stored at the given contiguous position in the array.
     * @param array
     * @param start
     * 	Starting element (inclusive)
     * @return
     * 	A short
     */
    private static short getShort(byte[] array, int start) {
    	if(array.length - start < 2) {
    		throw new RuntimeException("Cannot retrieve 2 bytes from array of length " + array.length + " with starting point " + start);
    	}
    	byte[] subArray = new byte[2];
    	System.arraycopy(array, start, subArray, 0, 2);
    	return ByteBuffer.wrap(subArray).getShort();
    }
}