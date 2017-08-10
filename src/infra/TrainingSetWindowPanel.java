package infra;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import dm.Source;
import dm.Source.Type;
import dm.Window;
import util.FileUtil;
import util.GuiUtil;

/**
 * This class provides a GUI component that visualises the current contents of the training set and
 * provides operations on it. This is for use in building a training set of {@link Window}s.
 *
 *
 * @author nrowell
 * @version $Id$
 */
public class TrainingSetWindowPanel extends JPanel {

	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 5222034304940297116L;

	/**
	 * The {@link List} of manually classified {@link Window}s.
	 */
	List<Window> classifiedWindows;
	
	/**
	 * Array of {@link JLabel}s displaying the number of each type of {@link Source}.
	 */
	JLabel[] counts;
	
	/**
	 * The directory in which to store output files.
	 */
	File outputDir;
	
	/**
	 * Main constructor for the {@link TrainingSetSourcePanel}, for working with {@link Source} data.
	 * 
	 * @param classifiedSources
	 * 	Reference to the {@link List<Source>}. The contents of this list will be displayed
	 * in the panel whenever the {@link TrainingSetSourcePanel#update()} method is called.
	 */
	public TrainingSetWindowPanel(List<Window> classifiedWindows, File outputDir) {
		this.classifiedWindows = classifiedWindows;
		this.outputDir = outputDir;
		
		// Panel displaying number of sources of each type
		counts = new JLabel[Type.values().length];
 		JPanel classPanel = new JPanel(new GridLayout(Type.values().length, 2));
 		classPanel.setBorder(BorderFactory.createTitledBorder("Source classifications"));
 		
 		for(int i=0; i<Type.values().length; i++) {
 			Type type = Type.values()[i];
 			JLabel label = new JLabel(type.toString());
 			label.setForeground(GuiUtil.getColor(type));
 			classPanel.add(label);
 			counts[i] = new JLabel("0");
 			classPanel.add(counts[i]);
 		}

 		JButton saveTrainingSetButton = new JButton("Save training set to file");
 		saveTrainingSetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				writeTrainingSetToFile();
			}});
 		
 		setLayout(new BorderLayout());
 		
 		add(classPanel, BorderLayout.CENTER);
 		add(saveTrainingSetButton, BorderLayout.SOUTH);
 		
		// Update with initial list contents
		update();
	}
	
	/**
	 * Writes all the manually classified {@link Window}s currently stored in the
	 * {@link GaiaWindowClassificationPanel#classifiedWindows} to file then clear
	 * the list.
	 */
	private void writeTrainingSetToFile() {
		// Write the list contents to a file then clear it
		int nSources = classifiedWindows.size();
		File output = new File(outputDir, "Window_"+nSources+".ser");
		FileUtil.serialize(output, classifiedWindows);
		classifiedWindows.clear();
		JOptionPane.showMessageDialog(this, "Written "+nSources+" classified Windows to "+output.getAbsolutePath(),
				"Writing training set to file...", JOptionPane.INFORMATION_MESSAGE);
		update();
	}
	
	/**
	 * Read the list contents and display in the frame
	 */
	public void update() {
		int[] srcCounts = new int[Type.values().length];
		for(Window window : classifiedWindows) {
			for(Source source : window.sources) {
				srcCounts[source.getType().ordinal()]++;
			}
		}
		for(int i=0; i<Type.values().length; i++) {
			counts[i].setText(String.format("%d", srcCounts[i]));
		}
	}

}