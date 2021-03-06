package gm.nodeode.model;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import gm.nodeode.io.ICourse;
import gm.nodeode.math.geom.Pt;

/**
 * Displays a simple circular node with a name
 * @author Garrett
 *
 */
public class OdeNode extends Visode {	
	public float x;
	public float y;
	public float r;
	
	public Color background = Color.WHITE;
	
	public OdeNode(String name, String display) {
		super(name);
		setDisplayName(display);
		this.r = 10;
	}
	
	public OdeNode(String name) {
		this(name, name);
	}
	
	public float getRadius() {
		return r;
	}
	
	public void setRadius(float r) {
		this.r = r;
	}
	
	public boolean contains(float x, float y) {
		x -= this.x;
		y -= this.y;
		return (x*x) + (y*y) <= r*r;
	}
	
	public OdeNode moveTo(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}
	public OdeNode moveBy(float dx, float dy) {
		this.x += dx;
		this.y += dy;
		return this;
	}
	
	
	public void draw(Graphics2D g) {
		if (Float.isInfinite(x) || Float.isInfinite(y) || Float.isNaN(x) || Float.isNaN(y)) {
			System.err.println("Warning: NaN pos: " + getUID());
			return;
		}
		
		int tx = (int) x;
		int ty = (int) y;
		int r  = (int)this.r;
		
		g.translate(tx, ty);
		
		if (getType() == Visode.TYPE_LINK || getType() == Visode.TYPE_SPACER) {
			// We're a link! Links are a lie!
			/*
			this.r = r = 3;
			g.setColor(Color.BLACK);
			
			if (getType() == Visode.TYPE_SPACER) {
				g.drawOval(-r, -r, r*2, r*2);
			} else {
				g.fillOval(-r, -r, r*2, r*2);
			}
			*/
		} else {
			g.setColor(background);
			g.fillOval(-r, -r, r*2, r*2);
			g.setColor(Color.BLACK);
			g.drawOval(-r, -r, r*2, r*2);
	
			FontMetrics fm = g.getFontMetrics();
			int width = 0;
			
			String[] parts = String.valueOf(getDisplayName()).split(" ");
			for (String s : parts) {
				width = Math.max(fm.stringWidth(s), width);
			}
			
			int y = -(fm.getHeight()*parts.length)/2 + fm.getAscent();
			for (String name : parts) {
				g.drawString(name, -fm.stringWidth(name)/2, y);
				y += fm.getHeight();
			}
			
			float tr = width/2+5;
			if (this.r < tr)
				this.r = tr;
		}
		
		g.translate(-tx, -ty);
	}

	@Override
	public Pt getCenter() {
		return Pt.P(x,y);
	}

	@Override
	public Visode setCenter(Pt p) {
		this.x = p.x;
		this.y = p.y;
		return this;
	}

	@Override
	public Visode moveBy(Pt p) {
		this.x += p.x;
		this.y += p.y;
		return this;
	}

	@Override
	public boolean contains(Pt p) {
		return contains(p.x, p.y);
	}

	@Override
	public Pt closestBorderPoint(Pt p) {
		Pt c = getCenter();
		float d = p.distance(c);
		if (d == 0)
			return c;
		
		return c.lerp(p, r/d);
	}

	@Override
	public Visode moveTo(Pt p) {
		return moveTo(p.x, p.y);
	}
}
