package gm.nodeode.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SaveImageButton extends JButton {
	private BufferedImage image;
	private ImageIcon icon;
	
	public SaveImageButton() {
		setPreferredSize(new Dimension(800, 800));
		
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					public void run() {
						save();
					}
				}).start();
			}
		});
	}
	
	public void setImage(final BufferedImage image) {
		this.image = image;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setIcon(new ImageIcon(image));
				setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
			}
		});
	}
	
	private volatile JFileChooser fc;
	public synchronized void save() {
		if (image == null) return;
		
		if (fc == null) {
			String[] paths = {
					System.getProperty("user.home") + File.separator + "Pictures",
					System.getProperty("user.home") + File.separator + "My Pictures",
					System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Pictures",
					System.getProperty("user.home") + File.separator + "My Documents" + File.separator + "My Pictures",
					System.getProperty("user.home") + File.separator + "Desktop",
					"."
			};
			File pics = null;
			for (String path : paths) {
				pics = new File(path);
				if (pics.exists())
					break;
			}
				
			fc = new JFileChooser(pics);
		}

		int approve = fc.showSaveDialog(this);
		if (approve != JFileChooser.APPROVE_OPTION)
			return;
		
		File f = fc.getSelectedFile();
		if (!f.getName().toLowerCase().endsWith(".png"))
			f = new File(f.getAbsolutePath() + ".png");
		if (f.exists()) {
			int overwrite = JOptionPane.showConfirmDialog(this, 
					"The file \"" + f.getName() + "\" already exists. Would you like to overwrite it?", 
					"Confirm Overwrite", JOptionPane.YES_NO_OPTION);
			if (overwrite != JOptionPane.YES_OPTION)
				return;
		}
		
		try {
			ImageIO.write(image, "png", f);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, 
					"Error writing file: " + e, 
					"Oops", JOptionPane.ERROR_MESSAGE);
		}
	}
}
