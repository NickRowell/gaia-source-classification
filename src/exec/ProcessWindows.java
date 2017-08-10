package exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import algo.SourceClassifier;
import algo.SourceDetector;
import algoimpl.SourceClassifierEmpirical;
import algoimpl.SourceDetectorWatershedSegmentation;
import dm.Source;
import dm.Source.Type;
import dm.Window;
import util.FileUtil;

/**
 * This is a simple demonstration of loading a bunch of {@link Window}s from a particular directory
 * and processing them one by one to perform source extraction and (possibly) classification.
 */
public class ProcessWindows {
	
	/**
	 * Main application entry point.
	 * @param args
	 * 	The command line arguments (ignored)
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// The directory containing all the files of {@link Window}s to process
		File inputDir = new File("/home/nrowell/Projects/SummerProjects/GaiaSourceClassification/data/Window/REV_1200_1400");
		
		// Directory to store the outputs
		File outputDir = new File("/home/nrowell/Projects/SummerProjects/GaiaSourceClassification/data/Source/REV_1200_1400");
		
		// Array of all files containing {@link Window}s
		List<File> windowFiles = FileUtil.listFilesRecursive(inputDir, FileUtil.windowFileFilter);
		
		// We'll use a source detection algorithm to identify sources in each window
		SourceDetector sourceDetector = new SourceDetectorWatershedSegmentation();
		
		// Get an empirical source classifier to classify the sources
//		SourceClassifier sourceClassifier = new SourceClassifierEmpirical();
		
		// Process each file in turn
		for(File windowFile : windowFiles) {
			
			
			// Load all the {@link Window}s from the file
			List<Window> windows = (List<Window>) FileUtil.deserialize(windowFile);
			
			// Buffer all the {@link Source}s before writing to file
			List<Source> sources = new LinkedList<>();
			
			// Compute the number of each type of source we found in this file
			int[] sourceCounts = new int[Type.values().length];
			
			// Process each {@link Window} in turn
			for(Window window : windows) {
				
				// Extract and optionally classify each {@link Source}
				for(Source source : sourceDetector.getSources(window)) {
					sources.add(source);
					
//					Type type = sourceClassifier.classifySource(source);
//					source.setType(type);
//					sourceCounts[type.ordinal()]++;
				}
			}
			
			// This splits filename "Window_78333600000000000_78334200000000000_31841.ser"
			// into ["Window", "78333600000000000", "78334200000000000", "31841.ser"]
			String[] parts = windowFile.getName().split("_");
			
			// Create output file to contain Sources
			// Filename is "Source_78333600000000000_78334200000000000_<# sources>.dat"
			String sourceFilename = "Source_"+parts[1]+"_"+parts[2]+"_"+sources.size()+".dat";
			
			FileOutputStream os = new FileOutputStream(new File(outputDir, sourceFilename));

			// Used to write binary Source data
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			for(Source source : sources) {
				// Write the sources to the byte array output stream
				out.write(source.toByteArray());
			}
			
			// Write the buffered sources to file
			out.writeTo(os);
			out.close();
			os.close();
			
			System.out.println("\nFound the following Sources in file "+windowFile.getName()+":");
			for(int i=0; i<Type.values().length; i++) {
				System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
			}
			
		}
		
	}
}