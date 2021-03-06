package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities related to {@link File} handling.
 *
 *
 * @author nrowell
 * @version $Id$
 */
public class FileUtil {

	/**
     * Logger
     */
    protected static Logger logger = Logger.getLogger(FileUtil.class.getCanonicalName());
    
    /**
     * A {@link FileFilter} used to filter files containing {@link dm.Window} data.
     */
    public static final FileFilter windowFileFilter = new FileFilter() {
		public boolean accept(File file) {
			if(!file.isFile()) {
				return false;
			}
			return file.getName().startsWith("Window_");
		}
	};

    /**
     * A {@link FileFilter} used to filter files containing {@link dm.Source} data.
     */
	public static final FileFilter sourceFileFilter = new FileFilter() {
		public boolean accept(File file) {
			if(!file.isFile()) {
				return false;
			}
			return file.getName().startsWith("Source_");
		}
	};
    
	/**
	 * Uses Java serialisation to write the Object to the File.
	 * 
	 * @param outputFile
	 * 	The {@link File} to write the {@link Object} to.
	 * @param object
	 * 	The {@link Object} to write to the {@link File}.
	 */
	public static void serialize(File outputFile, Object object) {
		
		// Write the accumulated {@link Window}s to file
		try (FileOutputStream file = new FileOutputStream(outputFile);
			 ObjectOutputStream output = new ObjectOutputStream(file);) {
			output.writeObject(object);
	    }  
	    catch(IOException ex){
	    	logger.log(Level.SEVERE, "Exception on writing to file.", ex);
	    }
	}
	
	/**
	 * Uses Java deserialization to read an object from file.
	 * @param outputFile
	 * 	The {@link File} to write the {@link Object} from.
	 * @return
	 * 	The {@link Object} read from the {@link File}.
	 */
	public static Object deserialize(File outputFile) {
		
		Object output = null;
		
		try (FileInputStream fileIn = new FileInputStream(outputFile);
			 ObjectInputStream in = new ObjectInputStream(fileIn);) {
			output = in.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return output;
	}
	
	/**
	 * List files in a given directory recursively.
	 * 
	 * @param parent
	 * 	The top level directory
	 * @param filter
	 * 	The {@link FileFilter} to apply to the returned file list
	 * @return
	 * 	A List of the all the files below the parent directory path that satisfy the file filter
	 */
	public static List<File> listFilesRecursive(File parent, FileFilter filter) {
		
		List<File> files = new LinkedList<>();
		
		for(File file : parent.listFiles()) {
			// If this is a regular file then conditionally add it to the output list
			if(file.isFile() && filter.accept(file)) {
				files.add(file);
			}
			// If this is a subdirectory then recursively search it
			if(file.isDirectory()) {
				files.addAll(listFilesRecursive(file, filter));
			}
		}
		return files;
	}
	
}