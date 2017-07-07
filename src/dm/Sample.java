package dm;

import java.io.Serializable;

/**
 * Class represents a single sample in a window.
 *
 * @author nrowell
 * @version $Id: Sample.java 470593 2015-12-09 18:54:47Z mdavidso $
 */
public class Sample implements Comparable<Sample>, Serializable {
	
    /**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 4675455671896840826L;
	
	/**
     * AL coordinate.
     */
    int al;
    
    /**
     * AC coordinate.
     */
    int ac;
    
    /**
     * Sample level.
     */
    double level;

    /**
     * Constructor for a {@link Sample}.
     *
     * @param al
     *            The AL coordinate.
     * @param ac
     *            The AC coordinate.
     * @param level
     *            The sample level.
     */
    public Sample(int al, int ac, double level) {
        this.al = al;
        this.ac = ac;
        this.level = level;
    }

    /**
     * Sets the AL coordinate.
     *
     * @param al
     *            The new AL coordinate
     */
    public void setAl(int al) {
        this.al = al;
    }

    /**
     * Gets the AL coordinate
     *
     * @return The AL coordinate of this sample
     */
    public int getAl() {
        return al;
    }

    /**
     * Sets the AC coordinate.
     *
     * @param ac
     *            The new AC coordinate
     */
    public void setAc(int ac) {
        this.ac = ac;
    }

    /**
     * Gets the AC coordinate
     *
     * @return The AC coordinate of this sample
     */
    public int getAc() {
        return ac;
    }

    /**
     * Sets the sample level.
     *
     * @param level
     *            The new level.
     */
    public void setLevel(double level) {
        this.level = level;
    }

    /**
     * Gets the sample level.
     *
     * @return The sample level
     */
    public double getLevel() {
        return level;
    }

    /**
     * Allows a Collection of Samples to be sorted into decreasing order of intensity.
     */
    @Override
    public int compareTo(Sample that) {

        if (this.level < that.level) {
            return 1;
        } else if (this.level > that.level) {
            return -1;
        }
        return 0;
    }

    /**
     * Override the equals method for {@link Sample}.
     */
    @Override
    public boolean equals(Object obj) {
   	 if (!(obj instanceof Sample))
            return false;
        if (obj == this)
            return true;

        Sample sample = (Sample) obj;
        
        if(this.al != sample.al) {
        	return false;
        }
        if(this.ac != sample.ac) {
        	return false;
        }
        if(this.level != sample.level) {
        	return false;
        }
        return true;
    }
}