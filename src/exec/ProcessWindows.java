package exec;

import java.io.File;
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
	 */
	public static void main(String[] args) {
		
		// The directory containing all the files of {@link Window}s to process
		File windowDirectory = new File("data/Window");
		
		// Array of all files containing {@link Window}s
		File[] files = windowDirectory.listFiles(FileUtil.windowFileFilter);
		
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
				
				// Now classify each {@link Source}
				for(Source source : sources) {
					Type type = sourceClassifier.classifySource(source);
					source.setType(type);
					
					sourceCounts[type.ordinal()]++;
				}
			}
			
			System.out.println("\nFound the following Sources in file "+file.getName()+":");
			for(int i=0; i<Type.values().length; i++) {
				System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
			}
			
		}
	}
}