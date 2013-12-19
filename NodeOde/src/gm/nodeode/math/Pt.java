package gm.nodeode.math;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This is my 3D point class, which is mostly used for 2D things.
 * Currently the canvas uses the third dimension to store the weight of the stroke.
 * 
 * I'm using some unconventional conventions I've developed over time to make
 * dealing with point/vector manipulation easier. To start, you may notice that I
 * initialize Pts by calling the static Pt.P(...) methods, rather than using constructors.
 * This is both because it makes chained calls (which I will describe below) more elegant.
 * I realize that this is somewhat terse, but I believe it facilitates rather than hampers readability.
 * 
 * I wrote this class so that I can chain calls on the same object, eg:
 * 
 * Pt.P(0,1,0).add(0,2).normalize3d().z(5);
 * 
 * Would create a new point <0,1,0>, add <0,2,0> to it, normalize it, and then set the z value to 5.
 * This allows me to perform complicated operations without producing lines and lines of dense math.
 * 
 * Generally, all functions either return the Pt object after completing, or return the floating-point
 * result of some operation (such as a dot-product, or a measure of the point's current magnitude).
 * 
 * @author Garrett
 *
 */
public class Pt implements Interpolable<Pt>, GP {
	public static final Pt A_X = Pt.P(1,0,0);
	public static final Pt A_Y = Pt.P(0,1,0);
	public static final Pt A_Z = Pt.P(0,0,1);
	
	public float x = 0, y = 0, z = 0;
	
	protected Pt(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	protected Pt(float x, float y) {
		this(x,y,0);
	}
	
	// Initializers
	public Point P() {
		return new Point((int)x, (int)y);
	}
	public Point2D.Float P2() {
		return new Point2D.Float(x, y);
	}

	public static Pt P(Point p) {
		return new Pt(p.x, p.y);
	}
	public static Pt P(Point2D.Float p) {
		return new Pt(p.x, p.y);
	}
	public static Pt P(Pt p) {
		return new Pt(p.x, p.y, p.z);
	}
	public static Pt P(float x, float y) {
		return new Pt(x,y);
	}
	public static Pt P(float x, float y, float z) {
		return new Pt(x,y,z);
	}
	public static Pt P(double x, double y) {
		return new Pt((float)x, (float)y);
	}
	public static Pt P(double x, double y, double z) {
		return new Pt((float)x, (float)y, (float)z);
	}
	
	public static Pt P(Point2D.Double p) {
		return new Pt((float)p.x, (float)p.y);
	}
	
	// Operations
	public Pt add(GP p) {
		this.x += p.x();
		this.y += p.y();
		this.z += p.z();
		return this;
	}
	public Pt add(float s, Pt p) {
		this.x += s * p.x;
		this.y += s * p.y;
		this.z += s * p.z;
		return this;
	}
	public Pt add(float s, Point p) {
		this.x += s * p.x;
		this.y += s * p.y;
		return this;
	}
	public Pt add(float x, float y, float z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	public Pt add(float x, float y) {
		return add(x,y,0);
	}
	public Pt add(Point p) {
		return add(p.x, p.y, 0);
	}
	public Pt set(float x, float y) {
		return set(x,y,0);
	}
	public Pt set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	public Pt set(GP p) {
		return set(p.x(), p.y());
	}
	public Pt set(Point p) {
		return set(p.x, p.y);
	}
	public Pt sub(GP p) {
		return add(-1, p);
	}
	public Pt sub(Point p) {
		return add(-1, p);
	}
	public Pt mul(Pt p) {
		this.x *= p.x;
		this.y *= p.y;
		this.z *= p.z;
		return this;
	}
	public Pt mul(float s) {
		this.x *= s;
		this.y *= s;
		this.z *= s;
		return this;
	}
	
	public float x() { return x; }
	public float y() { return y; }
	public float z() { return z; }
	
	public Pt x(float s) { this.x = s; return this; }
	public Pt y(float s) { this.y = s; return this; }
	public Pt z(float s) { this.z = s; return this; }
	public Pt rot2d() {
		return Pt.P(-y, x, z);
	}
	
	public Pt cross(Pt o) {
		return set(
				 (y*o.z - z*o.y),
				-(x*o.z - z*o.x),
				 (x*o.y - y*o.x)
		);
	}
	/**
	 * Removes all of that direction from this vector
	 * V = V-(V*A)
	 * @param axis
	 * @return
	 */
	public Pt flatten(Pt axis) {
		if (axis.mag2() != 1)
			axis = axis.d().normalize3d();
		
		return add(-dot(axis), axis);
	}
	/**
	 * Snaps this vector to the given axis:
	 * V = (V*A)*A
	 * @param axis
	 * @return
	 */
	public Pt snap(Pt axis) {
		axis = axis.d().normalize3d();
		return set(axis.mul(axis.dot(this)));
	}
	
	public Pt rotate(Pt axis) {
		return rotate(Mathf.PI/2, axis);
	}
	
	public Pt rot2d(float angle) {
		return rotate(angle, Pt.A_Z);
	}
	
	public Pt rotate(float angle, Pt axis) {
		if (axis.mag2() != 1)
			axis = axis.d().normalize3d();
		
		Pt I = d().cross(axis).normalize3d();
		if (I.mag2() == 0)
			return this; // We're parallel to the axis, rotation impossible.
		
		Pt J = I.d().cross(axis).normalize3d();
		
		float i = dot(I);
		float j = dot(J);
		float k = dot(axis);
		
		float m = Mathf.sqrt(i*i + j*j);

		
		float a = Mathf.atan2(j, i);
		a += angle;
		i = Mathf.cos(a) * m;
		j = Mathf.sin(a) * m;
		
		return set(0,0,0).add(i, I).add(j, J).add(k, axis);
	}

	@Override
	public float distance(Pt other) {
		return Distance(this, other);
	}

	public Pt lerp(Pt p, float s) {
		return set(this.x + s*(p.x-this.x), this.y + s*(p.y-this.y), this.z + s*(p.z-this.z));
	}
	
	public Pt slerp(Pt p, float s) {
		if (p.mag2() == 0 || mag2() == 0)
			return mul(s * (p.mag() - mag()));
		
		Pt axis = copy().cross(p).normalize3d();
		
		float angle = axis.mag2() == 0 ? 0 : s*Mathf.acos(dot(p));
		
		return rotate(angle, axis).mul(Mathf.pow(p.mag()/mag(), s));
	}
	
	// Unary Operators
	public Pt copy() {
		return new Pt(this.x, this.y, this.z);
	}
	public Pt d() { return copy(); } // for convenience
	public Pt normalize() {
		float m = mag2();
		if (m == 0)
			return this;
		return mul(1f/mag());
	}
	public Pt normalize3d() {
		float m = mag3d();
		if (m == 0)
			return this;
		return mul(1f/m);
	}
	
	// Measures
	public float dot(float x, float y, float z) {
		return this.x*x + this.y*y + this.z*z;
	}
	public float dot(float x, float y) {
		return dot(x,y,0);
	}
	
	public Pt onAxis(Pt axis) {
		axis = axis.copy().normalize();
		return axis.mul(axis.dot(this));
	}
	
	/**
	 * Returns the dot-product of this vector, and the 
	 * vector drawn from point A to point B.
	 * @param A
	 * @param B
	 * @return
	 */
	public float dot(Pt A, Pt B) {
		return dot(B.x-A.x, B.y-A.y, B.z-A.z);
	}
	
	public float det(float x, float y) {
		return dot(-y, x);
	}

	/**
	 * Returns the det-product of this vector, and the 
	 * vector drawn from point A to point B.
	 * @param A
	 * @param B
	 * @return
	 */
	public float det(Pt A, Pt B) {
		return det(B.x-A.x, B.y-A.y);
	}
	
	public float dot3d(Pt p) {
		return this.x*p.x + this.y*p.y + this.z*p.z;
	}
	public float dot(Pt p) {
		return this.x*p.x + this.y*p.y;
	}
	public float mag2() {
		return dot(this);
	}
	public float mag() {
		return (float) Math.sqrt(mag2());
	}
	public float mag3d() {
		return (float) Math.sqrt(dot3d(this));
	}
	public int ix() { return (int) x; }
	public int iy() { return (int) y; }
	public int iz() { return (int) z; }
	
	public float angle() {
		return Mathf.atan2(y, x);
	}
	
	private static Pt _ = new Pt(0,0);
	// Static operations (denoted by capital letter)
	public static Pt Lerp(Pt a, Pt b, float s) {
		return b.copy().sub(a).mul(s).add(a);
	}
	public static float Distance(Pt a, Pt b) {
		return a.copy().sub(b).mag();
	}
	
	public static boolean SameDir(Pt from, Pt a, Pt b) {
		Pt FA = a.copy().sub(from);
		Pt FB = b.copy().sub(from);

		return FA.dot(FB) > 0;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(10);
		sb.append("<");
		sb.append(x);
		sb.append(", ");
		sb.append(y);
		sb.append(", ");
		sb.append(z);
		sb.append(">");
		return sb.toString();
	}
	@Override
	public Pt toPt() {
		return this;
	}
	@Override
	public void fromPt(Pt P) {
		set(P);
	}
	@Override
	public Pt add(float s, GP o) {
		return add(s*o.x(), s*o.y(), s*o.z());
	}

}
