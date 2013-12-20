package gm.nodeode.model;

import java.awt.Graphics2D;

import gm.nodeode.math.geom.Pt;

/**
 * Visual nodes, node groups, etc
 * @author Garrett
 *
 */
public abstract class Visode {
	private final String uid;
	public Visode(String uid) {
		this.uid = uid;
	}
	
	public final String getUID() {
		return uid;
	}
	
	public abstract Pt getCenter();
	public abstract Visode setCenter(Pt p);
	
	public abstract Visode moveBy(Pt p);
	
	public abstract Visode moveTo(Pt p);
	
	public abstract boolean contains(Pt p);
	
	public abstract Pt closestBorderPoint(Pt p);
	
	public abstract void draw(Graphics2D g);
	
	public abstract float radius();
	
	public boolean prerender() {
		return false;
	}
	
	public String toString() {
		return uid;
	}
}
