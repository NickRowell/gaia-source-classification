package exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import dm.Source;
import dm.Source.Type;
import util.FileUtil;

/**
 * This is a simple demonstration of loading a bunch of {@link Source}s from a particular directory
 * and processing them.
 */
public class WriteSourcesToBinaryFile {
	
	/**
	 * The directory containing all the files of {@link Source}s to process.
	 */
	static File sourceDirectory = new File("data/Source/TrainingSet");
	
	/**
	 * Output text file location.
	 */
	static File outputFile = new File("data/Source/Training_Set.dat");
	
	/**
	 * Main application entry point.
	 * @param args
	 * 	The command line arguments (ignored)
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Open an output stream on the output file
		FileOutputStream os = new FileOutputStream(outputFile);
		
		// Create a ByteArrayOutputStream for writing the binary data to the file
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
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
				
				// Get the byte array representation for the {@link Source} and write to the byte output stream
				out.write(source.toByteArray());
				
				sourceCounts[source.getType().ordinal()]++;
			}
			
			// Write all {@link Source}s queued up in the byte array stream to the file
			out.writeTo(os);
			out.reset();
		}
		
		out.close();
		os.close();
		
		System.out.println("\nWritten the following Sources to file:");
		for(int i=0; i<Type.values().length; i++) {
			System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
		}
		
	}
}