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
		
		for (String v : access.getOdes()) {
			Visode o = access.find(v);
			Pt c = o.getCenter();
			if (Float.isNaN(c.x) || Float.isInfinite(c.x))
				continue;
			if (Float.isNaN(c.y) || Float.isInfinite(c.y))
				continue;
			
			graphMax.x = Mathf.max(c.x+o.radius(), graphMax.x);
			graphMax.y = Mathf.max(c.y+o.radius(), graphMax.y);
			
			graphMin.x = Mathf.min(c.x-o.radius(), graphMin.x);
			graphMin.y = Mathf.min(c.y-o.radius(), graphMin.y);
		}
		
		this.width = (int)(graphMax.x - graphMin.x) + padding*2;
		this.height = (int)(graphMax.y - graphMin.y) + padding*2;
		
		this.offx = -graphMin.ix();
		this.offy = -graphMin.iy();
	}
	
	public BufferedImage render() {		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
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
	
	public static BufferedImage layoutAndRender(OdeAccess access) {
		OdeAccess copy = new OdeManager(access);
		OdeLayout layout = new GansnerLayout(copy);
		layout.doLayout();
		
		GraphRenderer renderer = new GraphRenderer(copy);
		return renderer.render();
	}
}
