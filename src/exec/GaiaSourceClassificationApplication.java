package exec;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import infra.GaiaSourceClassificationPanel;
import util.GuiUtil;

/**
 * The main entry point for the Gaia source detection & classification application.
 *
 * @author nrowell
 * @version $Id$
 */
public class GaiaSourceClassificationApplication extends JFrame {
	
    /**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 3817647241577480212L;

    /**
     * Enumerated type to represent the two classification modes that the application can operate in:
     * 
     * MANUAL    -> source classifications are provided manually; all classified sources are saved to file as a training set.
     * AUTOMATIC -> source classifications are performed using a trained classifier; no output is saved to file.
     */
    public static enum Mode {MANUAL, AUTOMATIC}
    
	/**
     * Stores the path to the directory from which the last outputs were loaded.
     */
    private File currentInputDirPath;
    
	/**
	 * Main constructor.
	 */
	public GaiaSourceClassificationApplication() {
		
		// Ask the user in what mode to run
		Mode mode = GuiUtil.promptForClassificationMode();
		
		if(mode==Mode.MANUAL) {
			setTitle("Gaia Source Classification Application: manual classification mode");
		}
		else if(mode==Mode.AUTOMATIC) {
			setTitle("Gaia Source Classification Application: automatic classification mode");
		}
		
		// If we're in MANUAL mode then prompt the user to specify the output directory for
		// manually classified {@link Source}s.
		File output = null;
		if(mode==Mode.MANUAL) {
			output = GuiUtil.promptForOutputDirectory();
		}
		
		// Create a SourceDetectionClassificationTraining to display
		final GaiaSourceClassificationPanel app = new GaiaSourceClassificationPanel(mode, output);
		
		final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.getAccessibleContext().setAccessibleDescription("File Menu");
        final JMenuItem loadLibMenuItem = new JMenuItem("Load Windows", KeyEvent.VK_L);
        final JMenuItem saveImageMenuItem = new JMenuItem("Save image to file");
        final JMenuItem exitAppMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileMenu.add(loadLibMenuItem);
        fileMenu.add(saveImageMenuItem);
        fileMenu.add(exitAppMenuItem);
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        exitAppMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                System.exit(0);
            }
        });
		
        loadLibMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
            	
            	JFileChooser chooser = new JFileChooser(currentInputDirPath);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                int returnVal = chooser.showOpenDialog(menuBar);
                
                if(returnVal != JFileChooser.APPROVE_OPTION) {
                	return;
                }
            	
                // Remember directory for next time
                currentInputDirPath = chooser.getSelectedFile();
                
            	final SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    public Boolean doInBackground() {
                    	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						app.init(currentInputDirPath);
                    	return true;
                    }
                    @Override
                    public void done() {
						try {
							get();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
                		setCursor(Cursor.getDefaultCursor());
                    }
                };
                worker.execute();
            }
        });
        
        // Add a popup menu to the main sample display panel that provides an option to save the
        // current frame contents to an image file.
        saveImageMenuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				
				// Saves full JFrame to image:
//				Container c = getContentPane();
				// Saves just the window to an image:
				Container c = (Container)app.samplePanel.getComponent(0);
				
				BufferedImage im = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
				c.paint(im.getGraphics());
				
				// Use file chooser dialog to select file save location:
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Set output file...");
				chooser.addChoosableFileFilter(new FileFilter() {
					public boolean accept(File file) {
			    		String filename = file.getName();
			    		return filename.endsWith(".png");
			    	}
			    	public String getDescription() {
			    		return "*.png";
			    	}
				});
				
				int userSelection = chooser.showSaveDialog(GaiaSourceClassificationApplication.this);
				 
				if (userSelection == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					// Add extension '.png' if this was not given by the user
					String extension = "";
					int i = file.getName().lastIndexOf('.');
					if (i > 0) {
					    extension = file.getName().substring(i+1);
					}
					if (!extension.equalsIgnoreCase("png")) {
					    file = new File(file.toString() + ".png");
					}
					
					try {
						ImageIO.write(im, "PNG", file);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(GaiaSourceClassificationApplication.this, 
								"Error saving to file "+file.getAbsolutePath()+"!",
								"Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
        });

		setLayout(new GridLayout(1,1));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(app);
		pack();
        setLocationRelativeTo(null);
        setVisible(true);
	}
	
	/**
	 * Main entry point.
	 * 
	 * @param args
	 * 			The arguments (ignored)
	 */
	public static void main(final String[] args)
	{
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	new GaiaSourceClassificationApplication();
            }
        });
	}	
	
}