package exec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import dm.Source;
import dm.Source.Type;
import dm.Window;
import util.FileUtil;

/**
 * This class provides a short application for converting a set of {@link Window}s from a
 * serialized Java file to a binary file for use with other non-Java applications.
 */
public class WriteWindowsToBinaryFile {
	
	/**
	 * The directory containing all the files of {@link Window}s to process.
	 */
	static File windowDirectory = new File("data/Window/TrainingSet");
	
	/**
	 * Output text file location.
	 */
	static File outputFile = new File("data/Window/Training_Set.dat");
	
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
		
		// Array of all files containing {@link Window}s
		File[] files = windowDirectory.listFiles(FileUtil.windowFileFilter);

		// Compute the number of each type of source we found in this file
		int[] sourceCounts = new int[Type.values().length];
		
		// Process each file in turn
		for(File file : files) {
			
			// Load all the {@link Window}s from the file
			List<Window> windows = (List<Window>) FileUtil.deserialize(file);
			
			// Convert each {@link Window} to binary format
			for(Window window : windows) {
				
				// Get the byte array representation for the {@link Window} and write to the byte output stream
				out.write(window.toByteArray());
				
				for(Source source : window.sources) {
					sourceCounts[source.getType().ordinal()]++;
				}
			}
			
			// Write all {@link Window}s queued up in the byte array stream to the file
			out.writeTo(os);
			out.reset();
		}
		
		out.close();
		os.close();
		
		System.out.println("\nFound the following sources in the training set:");
		for(int i=0; i<Type.values().length; i++) {
			System.out.println(Type.values()[i] + "\t" + sourceCounts[i]);
		}
		
	}
}