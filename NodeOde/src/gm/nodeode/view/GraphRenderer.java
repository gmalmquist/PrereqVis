package gm.nodeode.view;

import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Graph;
import gm.nodeode.model.GansnerLayout;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeLayout;
import gm.nodeode.model.OdeManager;
import gm.nodeode.model.Visode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Takes in a connected Graph and plots all the nodes to a BufferedImage, after
 * laying them out with a Gasnser Layout.
 * @author Garrett
 *
 */
public class GraphRenderer {

	private OdeAccess access;
	
	private int padding = 10;
	private Stroke strokeSmall = new BasicStroke(1.2f);
	private Stroke strokeBig = new BasicStroke(2);
	
	private int width, height;
	private int offx, offy;
	
	public GraphRenderer(OdeAccess access) {
		this.access = access;
		
		calculateDimensions();
	}
	
	private void calculateDimensions() {
		Pt graphMax = Pt.P(0,0);
		Pt graphMin = Pt.P(0,0);
		
		boolean first = true;
		for (String v : access.getOdes()) {
			Visode o = access.find(v);
			Pt c = o.getCenter();
			if (Float.isNaN(c.x) || Float.isInfinite(c.x))
				continue;
			if (Float.isNaN(c.y) || Float.isInfinite(c.y))
				continue;
			
			float left = c.x - o.radius();
			float right = c.x + o.radius();
			float top = c.y - o.radius();
			float bottom = c.y + o.radius();
			
			if (first) {
				first = false;
				graphMax.set(left, top);
				graphMax.set(right, bottom);
			}
			
			graphMax.x = Mathf.max(right, graphMax.x);
			graphMax.y = Mathf.max(bottom, graphMax.y);
			
			graphMin.x = Mathf.min(left, graphMin.x);
			graphMin.y = Mathf.min(top, graphMin.y);
		}
		
		this.width = (int)(graphMax.x - graphMin.x) + padding*2;
		this.height = (int)(graphMax.y - graphMin.y) + padding*2;
		
		this.offx = -graphMin.ix();
		this.offy = -graphMin.iy();
	}
	
	public BufferedImage render() {		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		
		g.setColor(Color.WHITE);
		g.fillRect(-1, -1, width+2, height+2);
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.translate(offx, offy);
		
		// Draw links
		g.setStroke(strokeSmall);
		for (String vertex : access.getOdes()) {
			drawLinks(g, vertex);
		}

		LinkedList<Visode> renderList = new LinkedList<Visode>();
		// Draw nodes
		for (String vertex : access.getOdes()) {
			Visode o = access.find(vertex);
			if (o == null) break;
			if (o.prerender())
				renderList.push(o);
			else
				renderList.add(o);
		}

		for (Visode o : renderList) {
			o.draw(g);
		}

		g.dispose();
		return image;
	}

	private void drawLinks(Graphics2D g, String ode) {
		Visode O = access.find(ode);
		
		if (O == null)
			return;
		
		for (String parent : access.findParents(ode)) {
			Visode P = access.find(parent);
			if (P == null) {
				System.err.println("Graph is missing parents (" + parent + ")");
				continue;
			}
			
			Pt arrowA = O.closestBorderPoint(P.getCenter());
			Pt arrowB = P.closestBorderPoint(O.getCenter());
			
			g.setStroke(new BasicStroke(3));
			g.setColor(Color.WHITE);
			Art.arrow(g, arrowB, arrowA);
			
			g.setStroke(strokeSmall);
			g.setColor(Color.BLACK);
			Art.arrow(g, arrowB, arrowA);
			
		}
	}
	
	private static void initSizes(OdeAccess access) {
		// Make sure nodes know what size they are
		BufferedImage fake = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = fake.createGraphics();
		for (String v : access.getOdes()) {
			Visode o = access.find(v);
			if (o != null)
				o.draw(g);
		}
		g.dispose();
	}
	
	public static BufferedImage layoutAndRender(OdeAccess access) {
		// Init sizes for layout generations
		initSizes(access);
		
		OdeAccess copy = new OdeManager(access);
		OdeLayout layout = new GansnerLayout(copy);
		layout.doLayout();
		
		// Sizes might have changed due to virtual nodes
		initSizes(copy);
		
		GraphRenderer renderer = new GraphRenderer(copy);
		return renderer.render();
	}
}
