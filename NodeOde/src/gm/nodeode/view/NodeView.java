package gm.nodeode.view;

import gm.nodeode.NodeOde;
import gm.nodeode.math.Mathf;
import gm.nodeode.math.Pt;
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

public class NodeView extends JComponent implements OdeAccess {
	
	private static final float ANIMATION_TIME = 1.0f; // in seconds
	
	private final ConcurrentHashMap<String, List<String>> links;
	private final ConcurrentHashMap<String, Visode> odes;
	private final LinkedList<Visode> renderList;
	private final LinkedList<LerpOde> animations;
	
	private OdeLayout layout;
	
	private Pt mouse;
	
	private Visode dragging;
	private Pt dragoff;
	
	private Stroke strokeSmall = new BasicStroke(1.2f);
	private Stroke strokeBig = new BasicStroke(2);
	
	public NodeView() {
		setFocusable(true);
		
		setPreferredSize(new Dimension(800, 750));
		
		odes = new ConcurrentHashMap<String, Visode>();
		links = new ConcurrentHashMap<String, List<String>>();
		renderList = new LinkedList<Visode>();
		animations = new LinkedList<LerpOde>();
		
		layout = new FacepalmLayout(this);
		
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
			public void keyPressed(KeyEvent e) {
				NodeView.this.keyPressed(e.getKeyCode(), e.getKeyChar());
			}
		});
		
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					synchronized (odes) {
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
		NodeOde.debug("clear()");
		odes.clear();
		links.clear();
	}
	
	public void add(Visode ode) {
		odes.put(ode.getUID(), ode);
	}
	
	private void checkDragging() {
		Pt m = mouse.copy();
		for (String uid : odes.keySet()) {
			Visode node = odes.get(uid);
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
	
	private void drawLinks(Graphics2D g, String ode) {
//		System.out.println("Drawlinks " + ode);
		Visode O = find(ode);
//		System.out.println("Continuing... " + O);
		
		if (O == null)
			return;
		
		
		for (String parent : getParents(ode)) {
			Visode P = find(parent);
			if (P == null) {
				System.err.println("Graph is missing parents (" + parent + ")");
				continue;
			}
			Art.arrow(g, O.closestBorderPoint(P.getCenter()), 
					P.closestBorderPoint(O.getCenter()));
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

		// Draw links
		g.setStroke(strokeSmall);
		g.setColor(Color.BLACK);
		for (Visode o : renderList) {
			drawLinks(g, o.getUID());
		}
		
		// Draw nodes
		renderList.clear();
		for (String n : odes.keySet()) {
			Visode o = odes.get(n);
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
	}

	private synchronized void layoutStep() {

		synchronized (animations) {
			for (LerpOde anim : animations)
				if (!anim.isDone())
					anim.finish();
			animations.clear();
		}
		
		HashMap<String, Pt> positions = new HashMap<String, Pt>();
		for (String s : odes.keySet()) {
			Visode o = find(s);
			if (o instanceof OdeNode)
				positions.put(s, o.getCenter());
		}
		
		layout.doLayoutStep();
		
		synchronized (animations) {
			for (String s : positions.keySet()) {
				Pt A = positions.get(s);
				Pt B = find(s).getCenter();
				
				Pt AB = B.d().sub(A);
				if (AB.mag2() > 0) {
					animations.add(new LerpOde(find(s), A, B, ANIMATION_TIME));
				}
			}
		}
	}
	
	
	@Override
	public Visode find(String id) {
		if (!odes.containsKey(id)) {
			return null;
		}
		return odes.get(id);
	}

	@Override
	public void register(Visode ode) {
		odes.put(ode.getUID(), ode);
	}

	@Override
	public void remove(Visode ode) {
		odes.remove(ode.getUID());
	}

	@Override
	public void addParent(String ode, String parent) {
		if (!links.containsKey(ode))
			links.put(ode, new LinkedList<String>());
		
		links.get(ode).add(parent);
	}
	
	@Override
	public Iterable<String> getParents(String ode) {
		if (!links.containsKey(ode)) {
			return new LinkedList<String>();
		}
		
		return links.get(ode);
	}

	@Override
	public boolean hasParents(String ode) {
		return links.containsKey(ode) && links.get(ode).size() > 0;
	}

	@Override
	public boolean hasChildren(String ode) {
		for (String other : odes.keySet()) {
			if (!hasParents(other)) continue;
			if (links.get(other).contains(ode))
				return true;
		}
		return false;
	}

	@Override
	public Collection<String> getOdes() {
		return odes.keySet();
	}
}
