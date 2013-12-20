package gm.nodeode.math.geom;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Floating-point math so I don't have to keep
 * doing things like ((float) Math.cos(..)) everywhere
 * @author Garrett
 *
 */
public class Mathf {
	public static final float PI = (float) Math.PI;
	public static final float E = (float) Math.E;
	public static final float TAU = 2 * PI;

	public static final float sin(float a) { return (float) Math.sin(a); }
	public static final float cos(float a) { return (float) Math.cos(a); }
	public static final float tan(float a) { return (float) Math.tan(a); }
	
	public static final float atan(float a) { return (float) Math.atan(a); }
	public static final float acos(float a) { return (float) Math.acos(a); }
	public static final float asin(float a) { return (float) Math.asin(a); }
	public static final float atan2(float y, float x) { return (float) Math.atan2(y,x); }
	
	public static final float sqrt(float a) { return (float) Math.sqrt(a); }
	public static final float sq(float a) { return a*a; }
	
	public static final float pow(float a, float e) { return (float) Math.pow(a,e); }
	public static final float exp(float a) { return (float) Math.exp(a); }
	
	public static final float abs(float a) { return (float) Math.abs(a); }
	
	public static final float random() { return (float) Math.random(); }
	
	public static final float wrap(float theta) {
		while (theta < 0) theta += TAU;
		while (theta >= TAU) theta -= TAU;
		
		return theta;
	}
	
	public static final float gauss(float x) {
		return (1.0f / sqrt(2*PI)) * exp(-(x*x)/2);
	}
	
	public static final float gaussian() { 
		float avg = 0;
		for (int i = 0; i < 30; i++)
			avg += random();
		return avg / 50;
	}
	
	public static final int bsign(float a) {
		int s = sign(a);
		if (s == 0) return 1;
		return s;
	}
	
	public static final int sign(float a) {
		if (a < 0) return -1;
		if (a > 0) return 1;
		return 0;
	}

	public static final float clamp(float min, float max, float f) {
		if (f < min) return min;
		if (f > max) return max;
		return f;
	}
	
	public static final float max(float... fs) {
		float max = Float.NEGATIVE_INFINITY;
		for (float f : fs)
			if (f > max)
				max = f;
		return max;
	}
	public static final float min(float... fs) {
		float min = Float.POSITIVE_INFINITY;
		for (float f : fs)
			if (f < min)
				min = f;
		return min;
	}
	public static float lerp(float a, float b, float s) {
		return a + s*(b-a);
	}

	public static final float dist(float a, float b) {
		return a>b ? a-b : b-a;
	}
	
	public static final float dist(float x1, float y1, float x2, float y2) {
		return sqrt(sq(x1-x2) + sq(y1-y2));
	}
	
	public static final float distToSegment(Pt p, Pt a, Pt b) {
		Pt AB = Pt.P(b).sub(a);
		Pt AP = Pt.P(p).sub(a);
		return Pt.P(a).lerp(b, clamp(0,1,AP.dot(AB) / AB.mag2())).distance(p);
	}
	
	/**
	 * Returns the time of intersection (in units of vel) between point p 
	 * traveling with velocity vel towards a line defined by linePt and a
	 * normal vector lineNorm.
	 * @param p - initial position of traveling particle
	 * @param vel - velocity of traveling particle
	 * @param linePt - a point on the line
	 * @param lineNorm - the (unit-length!) normal vector of the line
	 * @return time of intersection, or Float.NaN if no intersection
	 */
	public static final float rayIntersectLine(Pt p, Pt vel, Pt linePt, Pt lineNorm) {
		Pt QP = p.d().sub(linePt);
		
		float dist = QP.dot(lineNorm);
		float rate = vel.dot(lineNorm);
		
		if (rate == 0) // Will never hit!
			return Float.NaN;
		
		return -dist/rate;
	}
	
	/**
	 * Returns the time of intersection between the ray and the line segment.
	 * @param p - initial position of traveling particle
	 * @param vel - velocity of traveling particle
	 * @param segA - one endpoint of the line segment
	 * @param segB - the other endpoint of the line segment
	 * @return time of intersection, or Float.NaN if no intersection
	 */
	public static final float rayIntersectSegment(Pt p, Pt vel, Pt segA, Pt segB) {
		Pt AB = segB.d().sub(segA);
		Pt N = AB.d().rot2d(PI/2).normalize3d();
		
		float time = rayIntersectLine(p, vel, segA, N);
		
		if (Float.isNaN(time) || Float.isInfinite(time))
			return time;
		
		// Point of collision
		Pt c = p.d().add(time, vel);

		// Check if the point of collision is actually on the line segment
		float s = c.d().sub(segA).dot(AB)/AB.mag2();
		if (s < 0 || s*s > AB.mag2())
			return Float.NaN;
		
		return time;
	}
	
	public static final float angleDiff(float theta1, float theta2) {
		float x = cos(theta2) - cos(theta1);
		float y = sin(theta2) - sin(theta1);
		
		if (x == 0 && y == 0)
			return 0;
		
		return atan2(y,x);
	}
	
	public static Rectangle bounds(Iterable<Pt> curve) {
		Pt min = Pt.P(Float.MAX_VALUE, Float.MAX_VALUE);
		Pt max = Pt.P(Float.MIN_VALUE, Float.MIN_VALUE);
		
		for (Pt p : curve) {
			if (p.x < min.x) min.x = p.x;
			if (p.y < min.y) min.y = p.y;
			if (p.x > max.x) max.x = p.x;
			if (p.y > max.y) max.y = p.y;
		}
		
		return new Rectangle(min.ix(), min.iy(), max.ix()-min.ix(), max.iy()-min.iy());
	}
	
	public static Rectangle bounds(Pt ... pts) {
		ArrayList<Pt> array = new ArrayList<Pt>(pts.length);
		for (Pt p : pts) array.add(p);
		return bounds(array);
	}
	
	public static float easyAnim(float s, float exp, float jiggleIn, float jiggleOut, float jiggleAmt) {
		s = clamp(0, 1, s);
		
		s = pow(s, exp); // ease amount
		s = sin(s * PI/2); // ease-in-ease-out
		
		if (s <= jiggleIn) { // anticipation
			float f = s / jiggleIn;
			float a = 0.0f;
			float b = -jiggleAmt / (jiggleIn);
			float c = 1.0f;
			
			s = jiggleIn * lerp(lerp(a, b, f*2), lerp(b, c, f*2-1), f);
		} else if (s >= jiggleOut) { // follow-through
			float f = (s - jiggleOut) / (1.0f - jiggleOut);
			float a = 0.0f;
			float b = 1.0f + jiggleAmt / (1.0f - jiggleOut);
			float c = 1.0f;
			
			s = jiggleOut + (1.0f - jiggleOut) * lerp(lerp(a, b, f*2), lerp(b, c, f*2-1), f);
		}
		
		return s;
	}
	
	public static Rectangle bounds(Rectangle a, Rectangle b) {
		ArrayList<Pt> array = new ArrayList<Pt>(8);
		int i = 0;
		
		array.add(Pt.P(a.x, a.y));
		array.add(Pt.P(a.x+a.width, a.y));
		array.add(Pt.P(a.x, a.y+a.height));
		array.add(Pt.P(a.x+a.width, a.y+a.height));

		array.add(Pt.P(b.x, b.y));
		array.add(Pt.P(b.x+b.width, b.y));
		array.add(Pt.P(b.x, b.y+b.height));
		array.add(Pt.P(b.x+b.width, b.y+b.height));
		
		return bounds(array);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Interpolable average(Interpolable ... pts) {
		int L = 0;
		int z = -1;
		for (Interpolable p : pts) {
			if (L == 0) z++;
			if (p != null) {
				L++;
			}
		}
		
		if (L == 0)
			return null;
		
		
		Interpolable M = pts[z].copy().add(-1, pts[0]);
		float a = 1.0f / L;
		for (int i = z; i < pts.length; i++) {
			if (pts[i] == null) continue;
			M.add(a, pts[i]);
		}
		
		return M;
	}
	
	public static Pt cartesian(float a, float r) {
		return Pt.P(r*cos(a), r*sin(a));
	}
	
	public static float area(Rectangle r) {
		return r.width * r.height;
	}
	
	public static Interpolable bezier(Interpolable a, Interpolable b, float t) {
		return lerp(a, b, t);
	}
	public static Interpolable bezier(Interpolable a, Interpolable b, Interpolable c, float t) {
		return bezier(bezier(a, b, t), bezier(b, c, t), t);
	}
	public static Interpolable bezier(Interpolable a, Interpolable b, Interpolable c, Interpolable d, float t) {
		return bezier(bezier(a, b, c, t), bezier(b, c, d, t), t);
	}
	public static Interpolable bezier(Interpolable a, Interpolable b, Interpolable c, Interpolable d, Interpolable e, float t) {
		return bezier(bezier(a, b, c, d, t), bezier(b, c, d, e, t), t);
	}
	
	/**
	 * Returns the time of collision between circle (A, a) moving with
	 * velocity V and static circle (B, b).
	 * @param A - center of circle A
	 * @param a - radius of circle A
	 * @param V - velocity of circle A
	 * @param B - center of circle B
	 * @param b - radius of circle B
	 * @return A non-negative float if collision is found, 
	 * NaN if collision is impossible, and a negative result 
	 * if collision is only possible in the past.
	 */
	public static float circleCircleCollision(Pt A, float a, Pt V, Pt B, float b) {		
		Pt QP = B.d().sub(A);
		
		float qA = V.dot(V);
		float qB = 2*V.dot(QP);
		float qC = QP.dot(QP) - (a+b)*(a+b);
		
		float det = qB*qB - 4*qA*qC;
		if (det < 0)
			return Float.NaN;
		
		float t0 = (-qB - sqrt(det))/(2*qA);
		float t1 = (+qB - sqrt(det))/(2*qA);
		
		if (t0 < 0) return t1;
		if (t1 < 0) return t0;
		
		return min(t0, t1);
	}
	
	public static Pt reflect(Pt V, Pt N) {
		Pt I = V.d();
		if (N.mag2() != 1)
			N = N.d().normalize();
		return I.add(-2*V.dot(N), N);
	}
	
	/**
	 * Moves 'a' to the location of 'b'
	 * @param a
	 * @param b
	 */
	public static void set(Interpolable a, Interpolable b) {
		a.add(-1, a).add(1, b);
	}
	
	public static Interpolable lerp(Interpolable A, Interpolable B, float s) {
		Interpolable T = B.copy().add(-1,A);
		T.add(s-1, T);
		return T.add(1,A);
	}
	
	public static Interpolable lerp(Interpolable A, Interpolable B, Interpolable C, float s) {
		return lerp(lerp(A,B,s*2), lerp(B,C,s*2-1), s);
	}
	
	public static Interpolable rayt(Interpolable A, Interpolable R, float t) {
		return A.copy().add(t, R);
	}
	
	public static float magnitude(Interpolable P) {
		return P.copy().add(-1, P).distance(P);
	}
	public static Interpolable normalize(Interpolable P) {
		Interpolable K = P.copy().add(-1, P);
		float d = K.distance(P);
		if (d == 0) 
			return K;
		return K.add(1.0f/d, P);
	}
	
}
