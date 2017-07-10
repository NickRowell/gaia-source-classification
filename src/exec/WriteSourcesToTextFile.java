package exec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import dm.Source;
import dm.Source.Type;
import util.FileUtil;

/**
 * This is a simple demonstration of loading a bunch of {@link Source}s from a particular directory
 * and processing them.
 */
public class WriteSourcesToTextFile {
	
	/**
	 * The directory containing all the files of {@link Source}s to process.
	 */
	static File sourceDirectory = new File("data/Source/TrainingSet");
	
	/**
	 * Output text file location.
	 */
	static File outputFile = new File("data/Source/Training_Set.txt");	
	
	/**
	 * Main application entry point.
	 * @param args
	 * 	The command line arguments (ignored)
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Open writer on the output file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
		
		// Array of all files containing {@link Source}s
		File[] files = sourceDirectory.listFiles(FileUtil.sourceFileFilter);
		
		// Compute the number of each type of source we found in this file
		int[] sourceCounts = new int[Type.values().length];
		
		// Process each file in turn
		for(File file : files) {
			
			// Load all the {@link Source}s from the file
			List<Source> sources = (List<Source>) FileUtil.deserialize(file);
			
			// Classify each {@link Source}
			for(Source source : sources) {
				out.write(source.toString());
				out.newLine();
				sourceCounts[source.getType().ordinal()]++;
			}
		}
		
		out.close();
		
		System.out.println("\nWritten the following Sources to file:");
		for(int i=0; i<Type.values().length; i++) {
			System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
		}
		
	}
}