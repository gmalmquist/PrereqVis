package gm.nodeode.model;

import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.view.Art;

import java.awt.Color;
import java.awt.Graphics2D;

public class OdeGroup extends Visode {
	private String[] kids;
	private OdeAccess manager;
	
	public OdeGroup(OdeAccess manager, String ... odes) {
		super(join(",", odes));
		this.kids = odes;
		this.manager = manager;
	}
	
	public String[] getChildren() { return kids; }

	public float radius() {
		return 1;
	}

	private static String join(String j, String[] s) {
		StringBuffer sb = new StringBuffer(s.length*10);
		for (String ss : s) {
			if (sb.length() > 0)
				sb.append(j);
			sb.append(ss);
		}
		return sb.toString();
	}


	@Override
	public Pt getCenter() {
		Pt miami = null;
		
		Pt[] cs = new Pt[kids.length];
		for (int i = 0; i < cs.length; i++) {
			Visode o = manager.find(kids[i]);
			if (o == null) {
				System.err.println("Missing child: " + kids[i]);
				cs[i] = miami;
				continue;
			}
			cs[i] = manager.find(kids[i]).getCenter();
		}
		
		Pt p = (Pt) Mathf.average(cs);
		if (p == null)
			return Pt.P(0,0);
		return p;
	}


	@Override
	public Visode setCenter(Pt p) {
		// NOT POSSIBLE
		return this;
	}


	@Override
	public Visode moveBy(Pt p) {
		// NOT POSSIBLE
		return this;
	}


	@Override
	public Visode moveTo(Pt p) {
		// NOPE
		return this;
	}


	@Override
	public boolean contains(Pt p) {
		return false;
	}


	@Override
	public Pt closestBorderPoint(Pt p) {
		return getCenter();
	}


	public boolean prerender() { return true; }
	
	@Override
	public void draw(Graphics2D g) {
		Pt c = getCenter();
		g.setColor(Color.BLUE);
		Art.show(g, c, 5);
		for (String s : kids) {
			Visode o = manager.find(s);
			if (o != null) {
				Art.line(g, c, o.getCenter());
			}
		}
	}
}
