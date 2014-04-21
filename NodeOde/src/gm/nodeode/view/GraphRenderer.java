package gm.nodeode.view;

import gm.debug.Blog;
import gm.nodeode.io.GraphIO;
import gm.nodeode.math.geom.Cubic;
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

	private static final boolean DEBUG_SPLINE_COLORS = false;
	
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
			
			float left = c.x - o.getRadius();
			float right = c.x + o.getRadius();
			float top = c.y - o.getRadius();
			float bottom = c.y + o.getRadius();
			
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
	
	
	private void cubic(Graphics2D g, Pt pA, Pt pB, Pt pC, Pt tD, Cubic f) {
		int n = (int) Math.max(3, pA.distance(pC)/3);
		
		for (int i = 0; i < n; i++) {
			Pt a = f.sample(pA, pB, pC, tD, (float)(i+0.0f)/n);
			Pt b = f.sample(pA, pB, pC, tD, (float)(i+1.0f)/n);

			g.drawLine(a.ix(), a.iy(), b.ix(), b.iy());
		}
	}
	
	private Pt tangent(String ode, boolean up) {		
		Visode O = access.find(ode);
		if (O == null) {
			return Pt.P(0,1,0);
		}
		
		
		if (O == null || O.getType() != Visode.TYPE_LINK)
			return Pt.P(0,1,0);
		
		Pt t = Pt.P(0,0,0);
		
		Pt centO = O.getCenter();
		for (String v : access.findChildren(ode)) {
			Pt centP = access.find(v).getCenter();
			t.add(centP.d().sub(centO).normalize3d());
		}
		for (String v : access.findParents(ode)) {
			Pt centP = access.find(v).getCenter();
			t.add(centP.d().sub(centO).normalize3d().mul(-1));
		}
		
		t.normalize3d();
		if (t.mag2() == 0) {
			t = Pt.P(0,1,0);
		}
		
		return t;
	}

	private void drawLinks(Graphics2D g, String ode) {
		Visode O = access.find(ode);
		
		if (O == null)
			return;
		
		Pt down = Pt.P(0,1,0);
		
		for (String parent : access.findParents(ode)) {
			Visode P = access.find(parent);
			if (P == null) {
				System.err.println("Graph is missing parents (" + parent + ")");
				continue;
			}
			
//			Pt arrowA = O.closestBorderPoint(P.getCenter());
//			Pt arrowB = P.closestBorderPoint(O.getCenter());
			
			Pt p0 = O.getCenter();
			Pt p1 = P.getCenter();
			
			Pt dir = p1.d().sub(p0);
//			p0.add(O.getRadius()*+Mathf.sign(dir.dot3d(down)), down);
//			p1.add(P.getRadius()*-Mathf.sign(dir.dot3d(down)), down);
			
			Pt B = p0.d().y(Mathf.lerp(p0.y, p1.y, 1 - (Mathf.random() * 0.0f + 0.1f)));
			Pt C = p1.d().y(Mathf.lerp(p1.y, p0.y, 1 - (Mathf.random() * 0.0f + 0.1f)));
			
			Pt t0 = tangent(ode, true);
			Pt t1 = tangent(parent, false);
			
			double hz = p1.x-p0.x;
			
			if (down.dot3d(t0) < 0) {
				t0.mul(-1);
			}
			if (down.dot3d(t1) < 0) {
				t1.mul(-1);
			}
			
			if (t0.x * hz < 0) {
				t0.x *= -1;
			}
			if (t1.x * hz < 0) {
				t1.x *= -1;
			}
			
			float scale = dir.mag3d()/1.5f;
			
			t0.mul(scale);
			t1.mul(scale);

			Stroke k = g.getStroke();
			
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(4));
			for (int i = 1; i < 2; i++) {
				Pt A = p0;
				Pt D = p1;
				if (i == 1) {
					g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					if (DEBUG_SPLINE_COLORS) {
						g.setColor(new Color[] {
								Color.BLACK, Color.BLUE,
								Color.GREEN, Color.RED,
								Color.CYAN, Color.MAGENTA
						}[(int)(Math.random()*5)]);
					} else {
						g.setColor(Color.BLACK);
					}
				} else {
					A = A.d().lerp(p1, 0.1f);
					D = D.d().lerp(p0, 0.1f);
				} 
	//			cubic(g, p0, t0, p1, t1, Cubic.HERMITE);
				cubic(g, A, B, C, D, Cubic.BEZIER);
			}
			
			g.setStroke(k);
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
	
	private static volatile int lindex = 0;
	public static synchronized BufferedImage layoutAndRender(OdeAccess access) {
		// Init sizes for layout generations
		initSizes(access);
		
		lindex++;

		Graph    conn = GraphIO.readGraph("graph-" + lindex + ".txt");
		Visode[] vert = GraphIO.readVisodes("vishy-" + lindex + ".txt");
		OdeAccess copy = null;
		
		if (conn != null && vert != null && false) {
			copy = new OdeManager(vert, conn);
		} else {
			copy = new OdeManager(access);
			OdeLayout layout = new GansnerLayout(copy, true);
			layout.doLayout();
			
			// Stick original display names back in
			for (String v : copy.getOdes()) {
				Visode cv = copy.find(v);
				Visode ov = access.find(v);
				if (ov == null || cv == null) {
					continue;
				}
				cv.setDisplayName(ov.getDisplayName());
			}
			
			try {
				GraphIO.saveGraph("graph-"+lindex+".txt", copy.copyGraph());
				GraphIO.saveVisodes("vishy-" + lindex + ".txt", copy.getVisodes());
			} catch (Exception e) {
				System.err.println("Error caching graphs: " + e);
			}
		}
		
		// Sizes might have changed due to virtual nodes
		initSizes(copy);

		for (Visode o : copy.getVisodes()) {
			if (o.getType() == Visode.TYPE_LINK)
				o.setRadius(0);
		}
		
		GraphRenderer renderer = new GraphRenderer(copy);
		return renderer.render();
	}
}
