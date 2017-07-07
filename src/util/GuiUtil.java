package util;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import dm.Sample;
import dm.Source;
import dm.Source.Type;
import exec.GaiaSourceClassificationApplication.Mode;

/**
 * Utilities related to source detection and classification GUI application.
 *
 * @author nrowell
 * @version $Id$
 */
public class GuiUtil {
	
	/**
	 * Size of the cells in the source image [pix].
	 */
	public static int cellWidthPix = 40;
	
	/**
	 * Size of the border between cells in the source image [pix].
	 */
	public static int interCellWidthPix = 1;

	/**
	 * Prompt the user to select what source classification mode to operate in.
	 * @return
	 * 	The {@link Mode} specifying the source classification mode.
	 */
	public static Mode promptForClassificationMode() {
		
		Object[] options = {"Manual", "Automatic"};
		int n = JOptionPane.showOptionDialog(null,
		    "Specify what source classification mode to operate in:"+
		    		"\n - Manual (generate a training set)"+
		    		"\n - Automatic (use a trained classifier)",
		    "Select classification mode",
		    JOptionPane.YES_NO_OPTION,
		    JOptionPane.QUESTION_MESSAGE,
		    null,
		    options,
		    options[0]);
		
		switch(n) {
		    case JOptionPane.YES_OPTION:
		    	return Mode.MANUAL;
		    case JOptionPane.NO_OPTION:
		    	return Mode.AUTOMATIC;
		    case JOptionPane.CLOSED_OPTION:
		    	// User closed the window; exit
		    	System.exit(0);
		}
		// NOTE: we shouldn't ever reach this code
		throw new IllegalStateException("Reached supposedly unreachable code. Hmm.");
	}
	
	/**
	 * Prompts the user to select a directory.
	 * @return
	 * 	The {@link File} specifying the directory selected by the user.
	 */
	public static File promptForOutputDirectory() {
		
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select the folder to save the classified Sources");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int returnVal = chooser.showOpenDialog(null);
        if(returnVal != JFileChooser.APPROVE_OPTION) {
        	// User hit 'cancel' or some other error occurred; exit
        	System.exit(0);
        }
        return chooser.getSelectedFile();
	}
	
	/**
	 * Creates an Image with the given dimenions and of the given colour. This is useful for configuring
	 * ImageIcons with the desired colour and size, for use in JLabels.
	 * @param w
	 * 	The image width
	 * @param h
	 * 	The image height
	 * @param r
	 * 	Red channel level (0-255)
	 * @param g
	 * 	Green channel level (0-255)
	 * @param b
	 * 	Blue channel level (0-255)
	 * @return
	 * 	An {@link Image} of the given dimensions and colour.
	 */
	public static Image createImage(int w, int h, int r, int g, int b) {
		
		// Encode 24-bit colour
		int rgb = (r << 16) + (g << 8) + b;
		
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		// Draw pixel intensities from integer array into RGB pixels
        for(int x=0; x<w; x++){
            for(int y=0; y<h; y++){
                image.setRGB(x, y, rgb);
            }
        }
        return image;
	}
	
	/**
	 * Encode Type as a color for representation on the GUI.
	 * Colour scheme obtained from colorbrewer2.org
	 * 
	 * @param type
	 * 	The Source type
	 * @return
	 * 	Color to represent that source
	 */
	public static Color getColor(Type type) {
		switch(type) {
			case STELLAR: return new Color(55,126,184);
			case COSMIC: return new Color(77,175,74);
			case SPIKE_AL:return new Color(152,78,163);
			case SPIKE_AC:return new Color(255,127,0);
			case SPIKE_DIAGONAL: return new Color(166,86,40);
			case UNKNOWN: return new Color(247,129,191);
		}
		return new Color(0,0,0);
	}
	
	/**
	 * Present the classified sources in a JPanel. This method works for 2D windows.
	 * 
	 * @param sources
	 * 	The classified sources
	 * @param fSamples
	 * 	The raw samples, in electrons
	 * @param alLength
	 * 	The window length (number of samples) in the AL direction
	 * @param acLength
	 * 	The window length (number of samples) in the AC direction
	 * @return
	 * 	A JPanel presenting visually the results of the source classification
	 */
	public static JPanel displaySources2D(List<Source> sources, float[] fSamples, int alLength, int acLength) {	
		
		JPanel jPanel = new JPanel(new GridBagLayout());
		
		// Scale factor for cell width/height; used to halve the size of cells for SM class 0 windows
		int width = cellWidthPix;
		int height = cellWidthPix;
		if(alLength == 40) {
			width  /= 2;
			height /= 2;
		}
		
		
		// Copy the source labels into an array to mirror the samples
		int[][] labels = new int[alLength][acLength];
		
		// Type of source in each cell
		Type[][] types = new Type[alLength][acLength];
		
		for(int al=0; al<alLength; al++) {
			for(int ac=0; ac<acLength; ac++) {
				// -1 mean 'no source'
				labels[al][ac] = -1;
				
			}
		}
		
		for(int s=0; s<sources.size(); s++) {
			Source source = sources.get(s);
			for(Sample sample : source.getSamples()) {
				labels[sample.getAl()][sample.getAc()] = s;
				types[sample.getAl()][sample.getAc()] = source.getType();
			}
		}
		
		// Get the min & max samples, for computing an appropriate colour stretch
		double maxSample = -Double.MAX_VALUE;
		double minSample = Double.MAX_VALUE;
		for(double sample : fSamples) {
			if(sample > maxSample) {
				maxSample = sample;
			}
			if(sample < minSample) {
				minSample = sample;
			}
		}
		
		// Make an image of the samples and segmentation.
		int rows = 2*acLength-1;
		int cols = 2*alLength-1;
		
		GridBagConstraints c = new GridBagConstraints();
		
		for(int row = 0; row < rows; row++) {
			
			// For the purposes of displaying the samples and the source segmentation
			// information, we alternate between two types of row: rows containing samples and the
			// inter-cell boundaries between them in the left-right direction, and rows
			// containing only the inter-cell boundaries between samples above and below (plus
			// a small cell with four inter-cell boundaries surrounding it):
			//
			//        -- AL -->
			//     ____________   _   ____________   _   ____________ 
			//    |            | | | |            | | | |            |
			//    |   Sample   | | | |   Sample   | | | |   Sample   |
			//    |    0       | | | |    1       | | | |    2       |  ...   <- sampleRow
			//    |            | | | |            | | | |            |
			// |  |____________| |_| |____________| |_| |____________|
			// |   ____________   _   ____________   _   ____________
			// AC |____________| |_| |____________| |_| |____________|       <- !sampleRow
			// |   ____________   _   ____________   _   ____________ 
			// V  |            | | | |            | | | |            |
			//    |            | | | |            | | | |            |
			//    |            | | | |            | | | |            |
			//    |            | | | |            | | | |            |
			//    |____________| |_| |____________| |_| |____________|
			
			boolean sampleRow = (row%2==0);
			
			for(int col = 0; col < cols; col++) {
				
				// Configure GridBagConstraints for cell placement
				c.gridx = col;
				c.gridy = row;
				
				if(sampleRow) {
					
					// Does this column correspond to a sample or to the boundary between two samples?
					boolean sampleCol = (col%2==0);
					
					if(sampleCol) {
						// Get the coordinates for this sample
						int ac = row/2;
						int al = col/2;
						
						boolean isSource = labels[al][ac]!=-1;
						
						// Get the source label
						String label = isSource ? Integer.toString(labels[al][ac]) : "";
						// Color for !isSource case does not matter as we don't draw anything
						Color labelColor = isSource ? getColor(types[al][ac]) : Color.BLACK;
						
						// Get the level of this sample
						int sampleIndex = al*acLength + ac;
						double sample = fSamples[sampleIndex];
						
						// Transform sample to 0:1 range
						
						// Linear stretch
//						sample = (sample-minSample)/(maxSample-minSample);
						
						// SQRT stretch
//						sample = Math.sqrt(sample-minSample)/Math.sqrt(maxSample-minSample);
						
						// Fourth root stretch
						sample = Math.pow(sample-minSample, 0.25)/Math.pow(maxSample-minSample, 0.25);
						
						// Log stretch
//						sample = Math.log(sample-minSample)/Math.log(maxSample-minSample);
						
						// Scale observed range of samples to 0:255 for representation
						// as a greyscal image. In future, could do more sophisticated colour levels.
						int grey = (int) Math.rint((sample * 255.0));
						int r = grey;
						int g = grey;
						int b = grey;
						
						// Create an ImageIcon suitable for representing this sample
						ImageIcon icon = new ImageIcon(GuiUtil.createImage(width, height, r, g, b));
						JLabel jLabel = new JLabel(label, icon, SwingConstants.CENTER);
						
						// Set the colour of the text based on the source type
						jLabel.setForeground(labelColor);
						jLabel.setHorizontalTextPosition(JLabel.CENTER);
						jLabel.setVerticalTextPosition(JLabel.CENTER);
						
						jPanel.add(jLabel, c);
					}
					else {
						// This column contains the horizontal inter-cell boundary between two samples.
						boolean sourceEdge = labels[col/2][row/2] != labels[col/2 + 1][row/2];
						int r = sourceEdge ? 255 : 0;
						int g = sourceEdge ?   0 : 0;
						int b = sourceEdge ?   0 : 0;
						
						ImageIcon icon = new ImageIcon(GuiUtil.createImage(interCellWidthPix, height, r, g, b));
						JLabel jLabel = new JLabel(icon);
						
						jPanel.add(jLabel, c);
					}
				}
				else {
					boolean sampleBoundary = (col%2==0);
					
					if(sampleBoundary) {
						// Cell lies on the vertical inter-cell boundary between two samples
						boolean sourceEdge = labels[col/2][row/2] != labels[col/2][row/2 + 1];
						int r = sourceEdge ? 255 : 0;
						int g = sourceEdge ?   0 : 0;
						int b = sourceEdge ?   0 : 0;
						
						ImageIcon icon = new ImageIcon(GuiUtil.createImage(width, interCellWidthPix, r, g, b));
						JLabel jLabel = new JLabel(icon);
						
						jPanel.add(jLabel, c);
					}
					else {
						// Cell contains the small square filler region
						// The east, west, north & south cells relative to this cell all contain the boundaries
						// between samples. Need to compare SW&NE, and SE&NW samples to determine if this cell
						// lies on a corner between sources.
						boolean sourceEdge = (labels[col/2 + 1][row/2] != labels[col/2][row/2 + 1]) ||
								             (labels[col/2][row/2] != labels[col/2 + 1][row/2 + 1]);
						int r = sourceEdge ? 255 : 0;
						int g = sourceEdge ?   0 : 0;
						int b = sourceEdge ?   0 : 0;
						
						ImageIcon icon = new ImageIcon(GuiUtil.createImage(interCellWidthPix, interCellWidthPix, r, g, b));
						JLabel jLabel = new JLabel(icon);
						
						jPanel.add(jLabel, c);
					}
					
				}
			}
		}
		return jPanel;
	}
	
}
