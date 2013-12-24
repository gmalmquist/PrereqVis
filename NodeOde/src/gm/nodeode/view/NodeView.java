package gm.nodeode.view;

import gm.nodeode.NodeOde;
import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.model.FacepalmLayout;
import gm.nodeode.model.Visode;
import gm.nodeode.model.OdeLayout;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeNode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

/**
 * Original view to animate the display of graphs; currently unused in favor of simply
 * generated bufferedimages.
 * @author Garrett
 *
 */
public class NodeView extends JComponent {
	
	private static final float ANIMATION_TIME = 1.0f; // in seconds
	private static final boolean HIGHLIGHT_CROSSINGS = false;
	
	private OdeAccess access;
	private OdeLayout layout;
	
	private final LinkedList<Visode> renderList;
	private final LinkedList<LerpOde> animations;
	
	private final Object renderLock = new Object();
	
	private Pt mouse;
	
	private Visode dragging;
	private Pt dragoff;
	
	private Stroke strokeSmall = new BasicStroke(1.2f);
	private Stroke strokeBig = new BasicStroke(2);
	
	public NodeView(OdeAccess access, OdeLayout layout) {
		this.access = access;
		this.layout = layout;
		
		setFocusable(true);
		
		setPreferredSize(new Dimension(800, 750));
		renderList = new LinkedList<Visode>();
		animations = new LinkedList<LerpOde>();
		
		mouse = Pt.P(0,0);
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				requestFocus();
			}
			
			public void mousePressed(MouseEvent e) {
				checkDragging();
			}
			
			public void mouseReleased(MouseEvent e) {
				releaseDragging();
			}
		});
		
		addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				new Thread(new Runnable() { public void run() {
				NodeView.this.keyPressed(e.getKeyCode(), e.getKeyChar());
				}}).start();
			}
		});
		
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					synchronized (renderLock) {
						update();
						repaint();
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						
					}
				}
			}
		}).start();
	}
	
	public void clear() {
		access.clear();
	}
	
	private void checkDragging() {
		Pt m = mouse.copy();
		for (String uid : access.getOdes()) {
			Visode node = access.find(uid);
			if (node.contains(m)) {
				Pt p = node.getCenter();
				dragging = node;
				dragoff = Pt.P(p.x - mouse.x, p.y - mouse.y);
			}
		}
	}
	private void releaseDragging() {
		dragging = null;
	}
	
	private long msstart = -1;
	private long msend = -1;
	private long mspass = 0;
	private float timePassed = 0;
	private void update() {
		try {
			Point mos = MouseInfo.getPointerInfo().getLocation();
			Point cos = getLocationOnScreen();
			mouse.set((mos.x - cos.x - tx)/sx, (mos.y - cos.y - ty)/sy);
		} catch (Exception e) {
			
		}
		
		if (dragging != null) {
			dragging.moveTo(mouse).moveBy(dragoff);
		}
		
		msend = System.currentTimeMillis();
		if (msstart == msend && msstart == -1) {
			mspass = 0;
		} else {
			mspass = msend - msstart;
		}
		timePassed = mspass / 1000f;
		msstart = System.currentTimeMillis();
		
		synchronized (animations) {
			List<LerpOde> dead = new LinkedList<LerpOde>();
			for (LerpOde anim : animations) {
				anim.update(timePassed);
				if (anim.isDone())
					dead.add(anim);
			}
			animations.removeAll(dead);
		}
	}
	
	private int crosses = -1;
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
			
			boolean cross = false;
			if (HIGHLIGHT_CROSSINGS) {
				for (String other : access.getOdes()) {
					for (String p : access.findParents(other)) {					
						if (layout.edgesCross(ode, parent, other, p)) {
							crosses++;
							cross = true;
							break;
						}
					}
				}
			}
			
			Pt arrowA = O.closestBorderPoint(P.getCenter());
			Pt arrowB = P.closestBorderPoint(O.getCenter());
			
			g.setStroke(new BasicStroke(3));
			g.setColor(Color.WHITE);
			Art.arrow(g, arrowB, arrowA);
			
			g.setStroke(strokeSmall);
			if (cross) g.setColor(Color.RED);
			else g.setColor(Color.BLACK);
			Art.arrow(g, arrowB, arrowA);
			
		}
	}

	private int tx, ty;
	private float sx=1, sy=1;
	@Override
	public void paint(Graphics gg) {
		Graphics2D g = (Graphics2D) gg;
		
		g.setColor(Color.WHITE);
		g.fillRect(-1, -1, getWidth()+2, getHeight()+2);
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		tx = getWidth()/2;
		ty = getHeight()/2;
		
		Pt graphMax = Pt.P(0,0);
		Pt graphMin = Pt.P(0,0);
		
		for (Visode o : renderList) {
			Pt c = o.getCenter();
			if (Float.isNaN(c.x) || Float.isInfinite(c.x))
				continue;
			if (Float.isNaN(c.y) || Float.isInfinite(c.y))
				continue;
			
			graphMax.x = Mathf.max(c.x, graphMax.x);
			graphMax.y = Mathf.max(c.y, graphMax.y);
			
			graphMin.x = Mathf.min(c.x, graphMin.x);
			graphMin.y = Mathf.min(c.y, graphMin.y);
		}
		
		sx = getWidth()  / (graphMax.x - graphMin.x + 50);
		sy = getHeight() / (graphMax.y - graphMin.y + 50);
		
		if (sx == 0) sx = 1;
		if (sy == 0) sy = 1;
		
		if (sx > 1) sx = 1;
		if (sy > 1) sy = 1;
		
		sx = sy = Mathf.min(sx, sy);

		
		tx += -sx * (graphMax.x + graphMin.x)/2;
		ty += -sy * (graphMax.y + graphMin.y)/2;
		
		g.translate(tx, ty);
		
		g.scale(sx, sy);

		g.setStroke(strokeBig);

		boolean nope = crosses < 0;
		crosses = 0;
		
		// Draw links
		g.setStroke(strokeSmall);
		for (Visode o : renderList) {
			drawLinks(g, o.getUID());
		}
		
		if (!nope) {
			System.out.println("Crosses: " + crosses);
		}
		crosses = -1;
		
		// Draw nodes
		renderList.clear();
		for (String n : access.getOdes()) {
			Visode o = access.find(n);
			if (o == null) break;
			if (o.prerender())
				renderList.push(o);
			else
				renderList.add(o);
		}

		for (Visode o : renderList) {
			o.draw(g);
		}
		
		g.scale(1/sx, 1/sy);

		
		boolean collisionTesting = false;
		if (collisionTesting) {
			Pt P = Pt.P(-100, 0);
			Pt Q = Pt.P( 100, 0);
			
			Pt M = mouse.d().add(-getWidth()/2, -getHeight()/2);
			
			Pt V = M.d().sub(P);
			
			g.setColor(Color.RED);
			Art.arrow(g, P, M);
			
			float p = 10;
			float q = 10;
			
			Color cP = Color.GREEN;
			Color cQ = Color.BLUE;
			Color cC = Color.BLACK;
			
			float t = Mathf.circleCircleCollision(P, p, V, Q, q);
			if (Float.isNaN(t)) {
				cP = Color.BLACK;
			} else {
				if (t < 0) cC = Color.GRAY;
				
				Pt C = P.d().add(t, V);
				g.setColor(cC);
				Art.show(g, C, p);
			}
			
			g.setColor(cP);
			Art.show(g, P, p);
			g.setColor(cQ);
			Art.show(g, Q, q);
		}
		
		g.translate(-tx, -ty);
	}
	
	private void keyPressed(int code, char key) {
		if (key == ']') 
			layoutStep();
		else if (key == '}')
			layout.doLayout();
		else if (key == 'x')
			crosses = 0;
	}

	private synchronized void layoutStep() {

		synchronized (animations) {
			for (LerpOde anim : animations)
				if (!anim.isDone())
					anim.finish();
			animations.clear();
		}
		
		HashMap<String, Pt> positions = new HashMap<String, Pt>();
		for (String s : access.getOdes()) {
			Visode o = access.find(s);
			if (o instanceof OdeNode)
				positions.put(s, o.getCenter());
		}
		
		layout.doLayoutStep();
		
		synchronized (animations) {
			for (String s : positions.keySet()) {
				Pt A = positions.get(s);
				Pt B = access.find(s).getCenter();
				
				Pt AB = B.d().sub(A);
				if (AB.mag2() > 0) {
					animations.add(new LerpOde(access.find(s), A, B, ANIMATION_TIME));
				}
			}
		}
	}
	
}
