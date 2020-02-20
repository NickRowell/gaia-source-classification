package infra;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import algoimpl.SourceClassifierEmpirical;
import algoimpl.SourceClassifierNN;
import algoimpl.SourceDetectorWatershedSegmentation;
import algo.SourceClassifier;
import algo.SourceDetector;
import dm.Source;
import dm.Source.Type;
import dm.Window;
import util.FileUtil;
import util.GaiaUtil;
import util.GuiUtil;
import util.LocalBkgUtils;

/**
 * This class provides the main GUI for the {@link exec.GaiaWindowClassificationApplication}.
 * 
 * @author nrowell
 * @version $Id$
 */
public class GaiaWindowClassificationPanel extends JPanel {
	
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 5162870620300906870L;

	/**
     * Logger
     */
    protected static Logger logger = Logger.getLogger(GaiaWindowClassificationPanel.class.getCanonicalName());
    
    /**
     * The JPanel showing the samples and source classification for the current window.
     */
    public JPanel samplePanel;
    
    /**
     * The JPanel showing the information for each classified source.
     */
    JPanel sourcePanel;
    
    /**
     * Handle to the current {@link File} we're handling.
     */
    File currentFile;
    
    /**
     * Iterator over all {@link File}s
     */
    ListIterator<File> fileIter;

    /**
     * Index of the current {@link File}.
     */
    int[] fileIdx = new int[1];
    
    /**
     * Total number of {@link File}s loaded.
     */
    int fileCount;
    
    /**
     * Handle to the current {@link Window} we're examining
     */
    Window currentWindow;
    
    /**
     * Iterator over the loaded {@link Window}s
     */
    ListIterator<Window> windowIter;
    
    /**
     * Index of the current {@link Window} in the original list
     */
    int[] windowIdx = new int[1];
    
    /**
     * Total number of {@link Window}s loaded.
     */
    int windowCount;

	/**
	 * Implementation of {@link SourceDetector} to use to perform source detection.
	 */
	SourceDetector sourceDetector = new SourceDetectorWatershedSegmentation();
	
	/**
	 * Implementation of {@link SourceClassifier} to use to perform source classification.
	 * Intention is that this can be swapped for different implementations easily.
	 */
	SourceClassifier sourceClassifier = new SourceClassifierEmpirical();
//	SourceClassifier sourceClassifier = new SourceClassifierNN();
	
	/**
	 * The {@link List} of all manually classified {@link Window}s.
	 */
	List<Window> classifiedWindows;
	
	/**
	 * The {@link TrainingSetWindowPanel} used to manipulate the training set.
	 */
	TrainingSetWindowPanel trainingSetPanel;
	
	/**
	 * Maps the number of sources fo each {@link Type} in the current {@link Window}. Use a one-element int array
	 * to store the number of sources.
	 */
	Map<Type, int[]> sourceCountsMap = new HashMap<>();
	
	/**
	 * Maps the source {@link Type} to the {@link JLabel} displaying the number counts.
	 */
	Map<Type, JLabel> sourceCountsStrings = new HashMap<>();
	
	/**
	 * The directory in which to store output files.
	 */
	File outputDir;
	
    /**
     * JLabels containing metadata for each {@link Window}; stored as fields of the class so
     * we can update them each time a {@link Window} is loaded.
     */
    JLabel fovLabel = new JLabel("-");
    JLabel rowLabel = new JLabel("-");
    JLabel stripLabel = new JLabel("-");
    JLabel gateLabel = new JLabel("-");
    JLabel acPosLabel = new JLabel("-");
    JLabel intTimeLabel = new JLabel("-");
    JLabel obsTimeLabel = new JLabel("-");
    JLabel locBkgLabel = new JLabel("-");
    
    /**
     * Constructor for the {@link GaiaWindowClassificationPanel}.
     * @param outputDir
     * 	The {@link File} specifying the directory to save outputs.
     */
	public GaiaWindowClassificationPanel(File outputDir) {
		
		this.outputDir = outputDir;
		
        // JButtons to control iteration through AstroObservations
		JButton prevFileButton = new JButton("<< Previous file");
 		JButton nextFileButton = new JButton("Next file >>");
		JButton prevWindowButton = new JButton("<< Previous Window");
 		JButton nextWindowButton = new JButton("Next Window >>");
 		JButton prevTransitButton = new JButton("<< Previous Transit");
 		JButton nextTransitButton = new JButton("Next Transit >>");
 		JButton nextCosmicButton = new JButton("Next cosmic ray >>");
 		JButton nextSpikeButton = new JButton("Next diffraction spike >>");
 		JButton nextUnknownButton = new JButton("Next unknown type >>");

		
		classifiedWindows = new LinkedList<>();
		trainingSetPanel = new TrainingSetWindowPanel(classifiedWindows, outputDir);
		JFrame trainingSetFrame = new JFrame("Training Set Contents");
		trainingSetFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		trainingSetFrame.add(trainingSetPanel);
		trainingSetFrame.pack();
		trainingSetFrame.setLocationRelativeTo(null);
		trainingSetFrame.setVisible(true);
		
 		samplePanel = new JPanel();
 		samplePanel.setBorder(BorderFactory.createTitledBorder("File X/TOTAL; Window X/TOTAL; "
 				+ "CCD_STRIP <STRIP>, Transit ID <TransitID>"));
 		Insets insets = samplePanel.getInsets();
 		// 20 cell widths: so we can display windows up to 20 samples wide comfortably
 		int prefferedWidth = 20*GuiUtil.cellWidthPix + 19*GuiUtil.interCellWidthPix + insets.left + insets.right;
 		int prefferedHeight = 12*GuiUtil.cellWidthPix + 11*GuiUtil.interCellWidthPix + insets.top + insets.bottom + 10;
 		samplePanel.setPreferredSize(new Dimension(prefferedWidth, prefferedHeight));
 		
 		sourcePanel = new JPanel(new BorderLayout());
 		sourcePanel.setBorder(BorderFactory.createTitledBorder("Source metadata"));

		JTabbedPane sourcesPanel = new JTabbedPane();
		sourcesPanel.add("-", getSourcePanel(null));
		sourcePanel.add(sourcesPanel);
		
 		JPanel windowMetaDataPanel = new JPanel(new GridLayout(8,2));
 		windowMetaDataPanel.setBorder(BorderFactory.createTitledBorder("Window metadata"));
 		windowMetaDataPanel.add(new JLabel("FOV:"));
 		windowMetaDataPanel.add(fovLabel);
 		windowMetaDataPanel.add(new JLabel("CCD row:"));
 	 	windowMetaDataPanel.add(rowLabel);
 		windowMetaDataPanel.add(new JLabel("CCD strip:"));
 		windowMetaDataPanel.add(stripLabel);
 		windowMetaDataPanel.add(new JLabel("CCD gate:"));
 		windowMetaDataPanel.add(gateLabel);
 		windowMetaDataPanel.add(new JLabel("AC pos (centre) [pix]:"));
 		windowMetaDataPanel.add(acPosLabel);
 		windowMetaDataPanel.add(new JLabel("Integration time [s]:"));
 		windowMetaDataPanel.add(intTimeLabel);
 		windowMetaDataPanel.add(new JLabel("Observation time [rev]:"));
 		windowMetaDataPanel.add(obsTimeLabel);
 		windowMetaDataPanel.add(new JLabel("Local background [e-]:"));
 		windowMetaDataPanel.add(locBkgLabel);
 		
 		// Set up button actions
 		prevFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateFile(false)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Window files!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		nextFileButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateFile(true)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Window files!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});

		prevWindowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateWindow(false)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Windows in this File!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		nextWindowButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateWindow(true)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Windows in this File!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		prevTransitButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateTransit(false)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Windows in this File!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		nextTransitButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(iterateTransit(true)) {
					updateGui();
				}
				else {
					JOptionPane.showMessageDialog(GaiaWindowClassificationPanel.this, "No more Windows in this File!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}});
		
		
 		nextCosmicButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(skipToNext(Type.COSMIC)) {
					updateGui();
				}
			}});
 		
 		nextSpikeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(skipToNext(Type.SPIKE_AC, Type.SPIKE_AL, Type.SPIKE_DIAGONAL)) {
					updateGui();
				}
			}});
 		
 		nextUnknownButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(skipToNext(Type.UNKNOWN)) {
					updateGui();
				}
			}});
 		
 		// Panel displaying colour scheme for different source classifications
 		JPanel classPanel = new JPanel(new GridBagLayout());
 		classPanel.setBorder(BorderFactory.createTitledBorder("Source classifications"));
 		
 		GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 5;
        c.ipady = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
	    c.fill = GridBagConstraints.BOTH;
 		c.gridy = 0;
 		
 		for(Type type : Type.values()) {
 			
 			JLabel label = new JLabel(type.toString());
 			label.setForeground(GuiUtil.getColor(type));

 			sourceCountsStrings.put(type, new JLabel(String.format("%4d", 0)));
 			sourceCountsMap.put(type, new int[1]);
 			
 			JButton add = new JButton("+");
 			JButton sub = new JButton("-");
 			
 			add.addActionListener(new ActionListener(){
 				@Override
 				public void actionPerformed(ActionEvent e) {
 					// Add one to the source type
 					int newCount = ++sourceCountsMap.get(type)[0];
 					sourceCountsStrings.get(type).setText(String.format("%4d", newCount));
 				}});
 			
 			sub.addActionListener(new ActionListener(){
 				@Override
 				public void actionPerformed(ActionEvent e) {
 					// Add one to the source type
 					int oldCount = sourceCountsMap.get(type)[0];
 					if(oldCount > 0) {
	 					int newCount = --sourceCountsMap.get(type)[0];
	 					sourceCountsStrings.get(type).setText(String.format("%4d", newCount));
 					}
 				}});
 			
 	        c.gridx = 0;
 			classPanel.add(label, c);
 			c.gridx = 1;
 			classPanel.add(new JLabel(type.getDescription()), c);
 			c.gridx = 2;
 			classPanel.add(sourceCountsStrings.get(type), c);
 			c.gridx = 3;
 			classPanel.add(add, c);
 			c.gridx = 4;
 			classPanel.add(sub, c);
 			c.gridy++;
 		}
 		
 		
        // Add button to add Window to the training set
        final JButton addToTrainingSetButton = new JButton("Add to training set");
        ActionListener bl = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) 
            {
            	if(currentWindow == null) {
            		return;
            	}
            	
            	// First check if we haven't already got this window in the list
            	if(classifiedWindows.contains(currentWindow)) {
            		JOptionPane.showMessageDialog(null, "Training set already contains this Window!",
            				"Training set conflict", JOptionPane.WARNING_MESSAGE);
            	}
            	else {
            		// Write all the Sources to the Window before saving to the list
            		for(Type type : sourceCountsMap.keySet()) {
            			for(int i=0; i<sourceCountsMap.get(type)[0]; i++) {
            				Source source = new Source();
            				source.setType(type);
            				currentWindow.sources.add(source);
            			}
            		}
            		
            		classifiedWindows.add(currentWindow);
            		trainingSetPanel.update();
            	}
            }
        };
        addToTrainingSetButton.addActionListener(bl);
 		
        c.gridx = 0;
        c.gridwidth = 5;
        classPanel.add(addToTrainingSetButton, c);
 		
 		JPanel buttonPanel = new JPanel(new GridLayout(5,2));
 		buttonPanel.add(prevFileButton);
 		buttonPanel.add(nextFileButton);
 		buttonPanel.add(prevTransitButton);
 		buttonPanel.add(nextTransitButton);
 		buttonPanel.add(prevWindowButton);
 		buttonPanel.add(nextWindowButton);
 		buttonPanel.add(nextCosmicButton);
 		buttonPanel.add(nextSpikeButton);
 		buttonPanel.add(nextUnknownButton);
 		
 		JPanel infoAndButtonPanel = new JPanel();
 		infoAndButtonPanel.setLayout(new BoxLayout(infoAndButtonPanel, BoxLayout.Y_AXIS));
 		infoAndButtonPanel.add(classPanel);
 		infoAndButtonPanel.add(windowMetaDataPanel);
 		infoAndButtonPanel.add(sourcePanel);
 		infoAndButtonPanel.add(buttonPanel);
 		
		// Layout the panel
		setLayout(new FlowLayout());
		add(samplePanel);
 		add(infoAndButtonPanel);
	}
	
	/**
	 * Resets the counters for the number of each {@link Source} {@link Type}s in the currently
	 * displayed {@link Window}.
	 */
	public void resetSourceCounts() {
		// Reset counters to zero
		for(int[] counter : sourceCountsMap.values()) {
			counter[0] = 0;
		}
		
		// Update counts
		if(currentWindow!=null) {
			for(Source source : currentWindow.sources) {
				sourceCountsMap.get(source.getType())[0]++;
			}
		}
		// Update text fields
		for(Type type : Type.values()) {
			sourceCountsStrings.get(type).setText(String.format("%4d", sourceCountsMap.get(type)[0]));
		}
	}

	/**
	 * Loads data from the given directory.
	 * @param windowDir
	 * 	The directory to load files containing {@link Window}s.
	 */
	public void init(File windowDir) {
		
		// Load all the files in this directory that contain Windows to a List
		List<File> windowFiles = new LinkedList<>();
		
		FileFilter directoryFilter = new FileFilter() {
			public boolean accept(File file) {
				if(!file.isFile()) {
					return false;
				}
				return file.getName().startsWith("Window_");
			}
		};

		File[] files = windowDir.listFiles(directoryFilter);
		
		for(File windowFile : files) {
			windowFiles.add(windowFile);
		}
		
		// Initialise iterator over Files
		fileIter = windowFiles.listIterator();
		fileIdx[0] = 0;
		fileCount = windowFiles.size();
		
		// Initialise iteration over the data
 		iterateFile(true);
 		
        // Initialise with first object
        updateGui();
	}
	
	/**
	 * Process the current AstroObservation/strip and plot the results in the GUI.
	 */
	private void updateGui() {
		
		resetSourceCounts();
		
		if(currentWindow==null) {
			// No currentWindow (likely we loaded a file that didn't contain any)
			return;
		}
		
		List<Source> sources = sourceDetector.getSources(currentWindow);
		
		for(Source source : sources) {
			source.setType(sourceClassifier.classifySource(source));
		}
		
		double[] bkgAndError = LocalBkgUtils.estimateBackgroundAndError(currentWindow.samples);
		
		samplePanel.setBorder(BorderFactory.createTitledBorder("File "+fileIdx[0]+"/"+fileCount+"; Window "
				+windowIdx[0]+"/"+windowCount+"; CCD_STRIP: "+GaiaUtil.stripNames[currentWindow.strip]+
				", Transit ID: "+currentWindow.transitId));
		
		samplePanel.removeAll();
		samplePanel.add(GuiUtil.displaySources2D(sources, currentWindow.samples, currentWindow.alSamples, currentWindow.acSamples));
		samplePanel.validate();
		
		// Update metadata/flags:
 		fovLabel.setText(String.format("FOV%d",currentWindow.fov));
 	    rowLabel.setText(String.format("ROW%d",currentWindow.row));
 	    stripLabel.setText(String.format("%s",GaiaUtil.stripNames[currentWindow.strip]));
 	    gateLabel.setText(String.format("%s",GaiaUtil.gateNames[currentWindow.gate]));
 	    acPosLabel.setText(String.format("%d", currentWindow.acWinCoord));
 	    intTimeLabel.setText(String.format("%f",currentWindow.intTime));
 	    obsTimeLabel.setText(String.format("%f",currentWindow.obmtRev));
 	    locBkgLabel.setText(String.format("%.5f +/- %.5f", bkgAndError[0], bkgAndError[1]));
 		
		// Update source panel
		JTabbedPane sourcesPanel = new JTabbedPane();
		
		if(sources.isEmpty()) {
			// Fill panel with placeholder fields in case of no sources, in order to
			// avoid resizing the GUI
			sourcesPanel.add("-", getSourcePanel(null));
		}
		else {
			for(int s=0; s<sources.size(); s++) {
				Source source = sources.get(s);
				sourcesPanel.add(Integer.toString(s), getSourcePanel(source));
			}
		}
		sourcePanel.removeAll();
		sourcePanel.add(sourcesPanel);
		sourcePanel.validate();
		
		repaint();
	}
	
	/**
	 * Presents a summary of the {@link Source} statistics in a {@link JPanel}.
	 * @param source
	 * 	The {@link Source} to present. This can be null, in which case the fields are
	 * filled with default values. This is useful when presenting the panel in the GUI
	 * before any data is loaded, so the forms can be sized correctly.
	 * @return
	 * 	A {@link JPanel} presenting a summary of the {@link Source} statistics.
	 */
	private JPanel getSourcePanel(Source source) {

		// Present source metadata in a JPanel
		JPanel sourceData = new JPanel(new GridLayout(7,2));
		
		sourceData.add(new JLabel("Source classification:"));
		
		sourceData.add(new JLabel(source!=null ? source.getType().toString() : "---"));
		
		sourceData.add(new JLabel("Integrated samples:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(source.getFlux()) : "---"));
		
		sourceData.add(new JLabel("Peak sample:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(source.getPeakFlux()) : "---"));
		
		sourceData.add(new JLabel("Peak-to-neighbour sample ratio:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(source.getFluxRatio()) : "---"));
		
		sourceData.add(new JLabel("Eigenvalue 1:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(source.getEigenvalues()[0]) : "---"));
		
		sourceData.add(new JLabel("Eigenvalue 2:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(source.getEigenvalues()[1]) : "---"));
		
		sourceData.add(new JLabel("Orientation (wrt AL direction) [deg]:"));
		sourceData.add(new JLabel(source!=null ? Double.toString(Math.toDegrees(source.getOrientation())) : "---"));
		
		return sourceData;
	}
	
	/**
	 * Hack used to deal with the fact that if we call next/prev/next on the list iterator,
	 * we get the same object: when changing direction we need to make an extra call to
	 * flush out the first returned object, which is the current one. This variable records
	 * which operation was called most recently on the corresponding ListIterator. It is a
	 * one-element array so that we can pass it into a method and update the value by reference.
	 */
	private boolean[] nextFileWasCalled = {false};
	/**
	 * @see #nextFileWasCalled
	 */
	private boolean[] prevFileWasCalled = {false};
	/**
	 * @see #nextFileWasCalled
	 */
	private boolean[] nextWindowWasCalled = {false};
	/**
	 * @see #nextFileWasCalled
	 */
	private boolean[] prevWindowWasCalled = {false};
	
	/**
	 * Generic method for iterating forwards through a ListIterator and loading the next object.
	 * @param it
	 * 	The {@link ListIterator} from which to retrieve the next object.
	 * @param nextWasCalled
	 * 	A single-element boolean array that indicates if the previous iteration on the list was in the forward
	 * direction. This is required in order to properly support next/prev/next sequences, where the default
	 * behaviour is to return the same object each time.
	 * @param prevWasCalled
	 * 	A single-element boolean array that indicates if the previous iteration on the list was in the backward
	 * direction. This is required in order to properly support next/prev/next sequences, where the default
	 * behaviour is to return the same object each time.
	 * @param idx
	 * 	Single-element int array used to increment/decrement a counter of the position within the iterator.
	 * @return
	 * 	The next object in the {@link ListIterator}
	 */
	private static <T> T loadNextObject(ListIterator<T> it, boolean[] nextWasCalled, boolean[] prevWasCalled, int[] idx) {
		if(!it.hasNext()) {
			return null;
		}
		nextWasCalled[0] = true;
		if(prevWasCalled[0])
		{
			prevWasCalled[0] = false;
			it.next();
		}
		idx[0]++;
		return it.next();
	}
	
	/**
	 * Generic method for iterating backwards through a ListIterator and loading the next object.
	 * @param it
	 * 	The {@link ListIterator} from which to retrieve the previous object.
	 * @param nextWasCalled
	 * 	A single-element boolean array that indicates if the previous iteration on the list was in the forward
	 * direction. This is required in order to properly support next/prev/next sequences, where the default
	 * behaviour is to return the same object each time.
	 * @param prevWasCalled
	 * 	A single-element boolean array that indicates if the previous iteration on the list was in the backward
	 * direction. This is required in order to properly support next/prev/next sequences, where the default
	 * behaviour is to return the same object each time.
	 * @param idx
	 * 	Single-element int array used to increment/decrement a counter of the position within the iterator.
	 * @return
	 * 	The previous object in the {@link ListIterator}
	 */
	private static <T> T loadPrevObject(ListIterator<T> it, boolean[] nextWasCalled, boolean[] prevWasCalled, int[] idx) {
		if(!it.hasPrevious()) {
			return null;
		}
		prevWasCalled[0] = true;
		if(nextWasCalled[0] )
		{
			// The most recent operation on the iterator was next(), i.e. we've changed
			// direction and need to flush one object
			nextWasCalled[0]  = false;
			it.previous();
		}
		
		// We need to watch out for a pathological case where if after the initial object is loaded the
		// user immediately clicks to load the previous object, there is none to flush out and the default
		// algorithm tries to access an object before the start of the list. Note that there is no 
		// equivalent in the loadNextAstroObservation method, because since we initialise at the start of the
		// list the problem is asymmetric and the issues never arises when iterating back and forth at the
		// end of the list. The issue also never arises at the start of the list after we've moved beyond
		// the first object.
		if(!it.hasPrevious()) {
			return null;
		}
		idx[0]--;
		return it.previous();
	}
	
	/**
	 * Loads the next or previous File from the ListIterator {@link #fileIter} to the {@link #currentFile} field.
	 * 
	 * @param isForward
	 * 	Specifies direction in which to iterate.
	 * @return
	 *  True if {@link #currentFile} has been updated, false if the iterator has
	 * been exhausted and there are no more Files present.
	 */
	private boolean iterateFile(boolean isForward)
	{
		if(fileIter==null) {
			// Woops - no inputs yet loaded
			return false;
		}
		
		File newFile = isForward ? loadNextObject(fileIter, nextFileWasCalled, prevFileWasCalled, fileIdx) : 
			  					   loadPrevObject(fileIter, nextFileWasCalled, prevFileWasCalled, fileIdx);
		if(newFile==null) {
			return false;
		}
		currentFile = newFile;

		// We got a new File; initialise the iteration over {@link Window}s.
		initialiseWindowIteration();
		
		return true;
	}
	
	/**
	 * Called when a new File has been loaded to {@link #currentFile}, in order to initialise the 
	 * {@link #windowIter} field and load the first object to {@link #currentWindow}.
	 */
	private void initialiseWindowIteration() {
		
		// We got a new File; now load all the {@link Window}s
		List<Window> windows = new LinkedList<>();
		
		if(currentFile.getName().endsWith(".ser")) {
			// File contains serialized Java objects
			@SuppressWarnings("unchecked")
			List<Window> wins = (List<Window>) FileUtil.deserialize(currentFile);
			windows.addAll(wins);
		}
		else if(currentFile.getName().endsWith(".dat")) {
			try {
				byte[] fileContent = Files.readAllBytes(currentFile.toPath());
				int[] p = {0};
				while(p[0] < fileContent.length) {
					windows.add(Window.fromByteArray(fileContent, p));
				}
			} catch (IOException e) {
				logger.severe("Exception reading file " + currentFile.getName());
			}
		}
		else {
			logger.severe("Couldn't interpret file " + currentFile.getName());
			return;
		}
		
		logger.info("Loaded "+windows.size()+" Windows from File "+currentFile.getName());
		
		// Initialise transit iteration
		nextWindowWasCalled[0] = false;
		prevWindowWasCalled[0] = false;
		windowIter = windows.listIterator();
		windowCount = windows.size();
		windowIdx[0] = 0;
		iterateWindow(true);
	}
	
	/**
	 * Loads the next or previous {@link Window} from the ListIterator {@link #windowIter}
	 * to the {@link #currentWindow} field.
	 * 
	 * @param isForward
	 * 	Specifies direction in which to iterate in list.
	 * @return
	 * 	True if the {@link #currentWindow} has been updated, false if we ran out of {@link Window}s and
	 * the {@link #currentWindow} was not updated.
	 */
	private boolean iterateWindow(boolean isForward)
	{
		if(windowIter==null) {
			// Woops - no inputs yet loaded
			return false;
		}
		
		Window newWindow = isForward ? loadNextObject(windowIter, nextWindowWasCalled, prevWindowWasCalled, windowIdx) : 
									loadPrevObject(windowIter, nextWindowWasCalled, prevWindowWasCalled, windowIdx);
		
		if(newWindow==null) {
			// No more objects in the given direction
			return false;
		}
		
		currentWindow = newWindow;
		return true;
	}
	

	/**
	 * Skips ahead or back to the next {@link Window} with a transit ID different from the
	 * {@link #currentWindow}.
	 * 
	 * @param isForward
	 * 	Specifies direction in which to iterate in list.
	 * @return
	 * 	True if the {@link #currentWindow} has been updated, false if we ran out of {@link Window}s and
	 * the {@link #currentWindow} was not updated.
	 */
	private boolean iterateTransit(boolean isForward)
	{
		if(windowIter==null) {
			// Woops - no inputs yet loaded
			return false;
		}
		
		Window newWindow = null;
		
		do {
			newWindow = isForward ? loadNextObject(windowIter, nextWindowWasCalled, prevWindowWasCalled, windowIdx) : 
									loadPrevObject(windowIter, nextWindowWasCalled, prevWindowWasCalled, windowIdx);
			
			if(newWindow==null) {
				// No more objects in the given direction
				return false;
			}
		}
		while(newWindow.transitId == currentWindow.transitId);
		
		currentWindow = newWindow;
		return true;
	}
	
	/**
	 * Iterate through the {@link File}s and {@link Window}s until we find a {@link Window}
	 * containing one of the given {@link Type} of object.
	 * 
	 * @param types
	 * 	Varargs Type giving the source types to look for
	 * @return
	 * 	Boolean indicating if we've found an object containing a source of the desired type,
	 * or have run out of objects (false). If true is returned, then the {@link #currentWindow}
	 * contains a source of the desired type in the {@link #currentStrip}.
	 */
	private boolean skipToNext(Type... types) {
		do {
			while(iterateWindow(true)) {
				List<Source> sources = sourceDetector.getSources(currentWindow);
				for(Source source : sources) {
					source.setType(sourceClassifier.classifySource(source));
				}
				// Check if any of the Sources match any of the Types we're looking for
				for(Source source : sources) {
					for(Type type : types) {
						if(source.getType()==type)
							return true;
					}
				}
			}
		}
		while(iterateFile(true));
		
		return false;
	}
	
}