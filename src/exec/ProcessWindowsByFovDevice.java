package exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import algo.SourceClassifier;
import algo.SourceDetector;
import algoimpl.SourceClassifierEmpirical;
import algoimpl.SourceDetectorWatershedSegmentation;
import dm.Source;
import dm.Source.Type;
import dm.Window;
import util.FileUtil;

/**
 * Application used to process {@link Window} data from a folder containing many files, convert it to
 * {@link Source}s and write these to file split by FOV and CCD ('device'), for further analysis.
 *
 * @author nrowell
 * @version $Id$
 */
public class ProcessWindowsByFovDevice {

	/**
	 * Main application entry point.
	 * @param args
	 * 	The command line arguments (ignored)
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// The directory containing all the files of {@link Window}s to process
		File inputDir = new File("/home/nrowell/Projects/SummerProjects/GaiaSourceClassification/data/Window/REV_1200_1400");
		
		// Directory to store the outputs. We split the Sources by CCD; so we can examine the variation across the focal plane
		File outputDir = new File("/home/nrowell/Projects/SummerProjects/GaiaSourceClassification/data/Source/REV_1200_1400");
		
		// Create map of output streams to files, where we'll store the sources for each device
		Map<Byte, Map<Byte, Map<Byte, FileOutputStream>>> filesByDevice = new TreeMap<>();
		
		filesByDevice.put((byte)0, new TreeMap<>());
		filesByDevice.put((byte)1, new TreeMap<>());
		
		for(byte ccd_row = 1; ccd_row < 8; ccd_row++) {
			
			Map<Byte, FileOutputStream> filesByRowFov1 = new TreeMap<>();
			Map<Byte, FileOutputStream> filesByRowFov2 = new TreeMap<>();
			
			for(byte ccd_strip = 5; ccd_strip < 13; ccd_strip++) {
				if(ccd_row==(byte)4 && ccd_strip==(byte)12) {
					// Skip nonexistant ROW4 AF9
					continue;
				}
				// Create the file to store Sources for this device
				File sourceFileFov1 = new File(outputDir, String.format("Source_FOV1_ROW%d_AF%d.dat", ccd_row, ccd_strip-3));
				File sourceFileFov2 = new File(outputDir, String.format("Source_FOV2_ROW%d_AF%d.dat", ccd_row, ccd_strip-3));
				
				// Open a FileOutputStream on it
				filesByRowFov1.put(ccd_strip, new FileOutputStream(sourceFileFov1));
				filesByRowFov2.put(ccd_strip, new FileOutputStream(sourceFileFov2));
			}
			filesByDevice.get((byte)0).put(ccd_row, filesByRowFov1);
			filesByDevice.get((byte)1).put(ccd_row, filesByRowFov2);
		}
		
		// Used to write binary Source data to the files
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// Array of all files containing {@link Window}s
		List<File> files = FileUtil.listFilesRecursive(inputDir, FileUtil.windowFileFilter);
		
		// We'll use a source detection algorithm to identify sources in each window
		SourceDetector sourceDetector = new SourceDetectorWatershedSegmentation();
		
		// Get an empirical source classifier to classify the sources
		SourceClassifier sourceClassifier = new SourceClassifierEmpirical();
		
		// Process each file in turn
		for(File file : files) {
			
			// Load all the {@link Window}s from the file
			List<Window> windows = (List<Window>) FileUtil.deserialize(file);
			
			// Compute the number of each type of source we found in this file
			int[] sourceCounts = new int[Type.values().length];
			
			// Process each {@link Window} in turn
			for(Window window : windows) {
				
				// Extract {@link Source}s within each {@link Window}
				List<Source> sources = sourceDetector.getSources(window);
				
				// Retrieve the FileOutputStream for the device on which this Window was observed
				FileOutputStream os = filesByDevice.get(window.fov).get(window.row).get(window.strip);
				
				// Now classify each {@link Source}
				for(Source source : sources) {
					
					// Write the sources to the byte array output stream
					out.write(source.toByteArray());
					
					Type type = sourceClassifier.classifySource(source);
					source.setType(type);
					
					sourceCounts[type.ordinal()]++;
				}
				
				// Write the buffered sources to file
				out.writeTo(os);
				out.reset();
			}
			
			System.out.println("\nFound the following Sources in file "+file.getName()+":");
			for(int i=0; i<Type.values().length; i++) {
				System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
			}
			
		}
		
		// Close output streams
		out.close();
		
		for(Byte ccd_row : filesByDevice.keySet()) {
			for(Byte ccd_strip : filesByDevice.get(ccd_row).keySet()) {
				filesByDevice.get((byte)0).get(ccd_row).get(ccd_strip).close();
				filesByDevice.get((byte)1).get(ccd_row).get(ccd_strip).close();
			}
		}
		
	}
}
