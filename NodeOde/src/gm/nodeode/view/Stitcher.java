package gm.nodeode.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Class for stitching multiple images together into a unified image, using 
 * a relatively naive but functional algorithm.
 * @author Garrett
 *
 */
public class Stitcher {

	public static BufferedImage stitch(BufferedImage ... sources) {
		Point[] positions = new Point[sources.length];
		
		Arrays.sort(sources, new Comparator<BufferedImage>() {
			public int compare(BufferedImage A, BufferedImage B) {
				return B.getHeight() - A.getHeight();
			}
		});
		
		int maxheight = sources[0].getHeight();
		int curwidth = 0;
		int x = sources[0].getWidth();
		int y = 0;
		
		positions[0] = new Point(0,0);
		
		for (int i = 1; i < sources.length; i++) {
			if (y + sources[i].getHeight() > maxheight) {
				y = 0;
				x += curwidth;
				curwidth = 0;
			}
			positions[i] = new Point(x, y);
			
			y += sources[i].getHeight();
			curwidth = Math.max(curwidth, sources[i].getWidth());
		}
		
		int width = x + curwidth;
		int height = maxheight;

		
		BufferedImage stitch = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = stitch.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(-1, -1, width+2, height+2);
		
		for (int i = 0; i < sources.length; i++) {
			g.drawImage(sources[i], positions[i].x, positions[i].y, null);
		}
		
//		g.setColor(Color.BLUE);
//		for (int i = 0; i < sources.length; i++) {
//			g.drawRect(positions[i].x, positions[i].y, sources[i].getWidth(), sources[i].getHeight());
//		}
		
		g.dispose();
		
		return stitch;
	}
}
