package gm.nodeode.model;

import java.awt.Graphics2D;

import gm.nodeode.math.geom.Pt;

/**
 * Visual nodes, node groups, etc
 * @author Garrett
 *
 */
public abstract class Visode {
	public static final int TYPE_NODE = 0, TYPE_SPACER = 1, TYPE_LINK = 2;
	
	private final String uid;
	private int type;
	public Visode(String uid) {
		this.uid = uid;
		this.type = TYPE_NODE;
	}
	
	public final String getUID() {
		return uid;
	}
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
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
