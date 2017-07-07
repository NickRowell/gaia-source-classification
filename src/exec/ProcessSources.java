package exec;

import java.io.File;
import java.util.List;

import dm.Source;
import dm.Source.Type;
import util.FileUtil;

/**
 * This is a simple demonstration of loading a bunch of {@link Source}s from a particular directory
 * and processing them.
 */
public class ProcessSources {
	
	/**
	 * Main application entry point.
	 * @param args
	 * 	The command line arguments (ignored)
	 */
	public static void main(String[] args) {
		
		// The directory containing all the files of {@link Source}s to process
		File sourceDirectory = new File("data/Source/TrainingSet");
		
		// Array of all files containing {@link Source}s
		File[] files = sourceDirectory.listFiles(FileUtil.sourceFileFilter);
		
		// Process each file in turn
		for(File file : files) {
			
			// Load all the {@link Source}s from the file
			List<Source> sources = (List<Source>) FileUtil.deserialize(file);
			
			// Compute the number of each type of source we found in this file
			int[] sourceCounts = new int[Type.values().length];
			
			// Classify each {@link Source}
			for(Source source : sources) {
				sourceCounts[source.getType().ordinal()]++;
			}
			
			System.out.println("\nFound the following Sources in file "+file.getName()+":");
			for(int i=0; i<Type.values().length; i++) {
				System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
			}
			
		}
	}
}