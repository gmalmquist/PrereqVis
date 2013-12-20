package gm.nodeode.math.geom;

import gm.nodeode.math.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a curve which takes in a list of abstract points (Interpolables),
 * and is capable of returning a smooth interpolation between them.
 * @author Garrett
 *
 * @param <P>
 */
public class Curve<P extends Interpolable<P>> implements ICurve<P> {
	private ArrayList<P> points;
	private float length = 0;
	
	private int smoothLength = -1;
	
	private boolean sampleLinear = false;
	
	public Curve() {
		points = new ArrayList<P>(64);
	}
	
	public Curve(Iterable<P> pts) {
		this();
		add(pts);
	}
	
	public Curve(P ... pts) {
		this();
		add(pts);
	}
	
	public void set(Curve<P> to) {
		if (to == this)
			return;
		
		this.points.clear();
		this.points.addAll(to.points);
		this.length = to.length;
	}
	
	public void add(Iterable<P> pts) {
		for (P p : pts)
			add(p);
	}
	
	public void add(P ... pts) {
		P last = null;
		if (points.size() > 0)
			last = points.get(points.size()-1);
		
		for (P p : pts) {
			if (last != null)
				length += last.distance(p);
			last = p;
			points.add(p);
		}
	}
	
	public void recalculateLength() {
		length = 0;
		P last = null;
		for (P p : this) {
			if (last != null)
				length += last.distance(p);
			last = p;
		}
	}
	
	public void setSmoothLength(int length) {
		this.smoothLength = length;
	}
	
	private Pair<Integer,Float> samplePos(float t) {
		if (points.isEmpty())
			return null;
		
		if (t < 0)
			return new Pair<Integer,Float>(0,0f);
		
		int i = 0;
		for (; i < points.size()-1 && t >= 0; i++) {
			P A = points.get(i);
			P B = points.get(i+1);
			float len = A.distance(B);
			if (len == 0)
				continue;
			
			if (len > t) {
				return new Pair<Integer, Float>(i, t/len);
			}
			
			t -= len;
		}
		
		return new Pair<Integer, Float>(points.size()-1, 0f);
	}
	
	public void shift(P by) {
		for (P p : points)
			p.add(1, by);
	}
	
	public Curve<P> differentiate() {
		List<P> pts = new ArrayList<P>(points.size());
		for (int i = 0; i < points.size(); i++) {
			pts.add(gradient(length() * i / (points.size()-1)));
		}
		return new Curve<P>(pts);
	}
	
	private P gradientDiscrete(int i) {
		if (points.size() <= 1)
			return null;
		
		if (i < 0) 
			i = 0;
		if (i >= points.size())
			i = points.size()-1;
		
		if (i == 0) {
			return points.get(1).copy().add(-1, points.get(0));
		} else if (i == points.size()-1) {
			return points.get(i).copy().add(-1, points.get(i-1));
		}

//		if (true) return points.get(i+1).copy().add(-1, points.get(i));
//		if (true) return points.get(i).copy().add(-1, points.get(i-1));
		
		return points.get(i+1).copy().add(-1, points.get(i-1)).mul(0.5f);
	}
	
	public P gradient(float t) {
		Pair<Integer, Float> pos = samplePos(t);
		if (pos == null) return null;
		
		int i = pos.a;
		float s = pos.b;

		return (P) Mathf.lerp(gradientDiscrete(i), gradientDiscrete(i+1), s);
	}
	
	/**
	 * t is expected to be between 0 and length.
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public P sample(float t) {
		if (points.isEmpty())
			return null;
		
		Pair<Integer, Float> pos = samplePos(t);
		int i = pos.a;
		float s = pos.b;

		if (i == points.size()-1)
			return (P) Mathf.lerp(points.get(i-1), points.get(i), 1f+s);
		
		if (sampleLinear)
			return sampleLinear(i, s);

		// Naive neville's interpolation on 3 pts
		P sample = null;
//		sample = sampleCubic(i, s);
//		sample = sampleSine(i, s);
//		sample = sampleLinear(i, s);
		sample = sampleHermite(i, s);
		return sample;
	}
	
	private P sampleHermite(int i, float s) {
		P A = points.get(i);
		P B = points.get(i+1);
		
		P gA = gradient(length() * (0+i) / (count()-1));
		P gB = gradient(length() * (1+i) / (count()-1));
		
		gA = gradientDiscrete(i+0);
		gB = gradientDiscrete(i+1);

		float t = s;
		
		return A.copy().add(-1, A)
				.add(2*t*t*t - 3*t*t +1, A)
				.add(t*t*t - 2*t*t + t, gA)
				.add(-2*t*t*t + 3*t*t, B)
				.add(t*t*t - t*t, gB);
	}
	
	private P sampleSine(int i, float s) {
		P A = points.get(i);
		P B = points.get(i+1);
		
		P gA = gradient(length() * (0+i) / (count()-1));
		P gB = gradient(length() * (1+i) / (count()-1));
		
		gA = gradientDiscrete(i+0);
		gB = gradientDiscrete(i+1);
		
		gA = (P) Mathf.normalize(gA).mul(A.distance(B));
		gB = (P) Mathf.normalize(gB).mul(A.distance(B));
		
		P L0 = (P) Mathf.rayt(A, gA, s);
		P L1 = (P) Mathf.rayt(B, gB, s-1);
		
		return (P) Mathf.lerp(L0, L1, Mathf.pow(Mathf.sin(s*Mathf.PI/2),2));
	}
	
	private P sampleCubic(int i, float s) {
		P A = points.get(i);
		P B = points.get(i+1);
		return (P) Mathf.lerp(A, B, i < points.size()-2 ? points.get(i+2) : (P) Mathf.lerp(A,B,2), s/2);
	}
	
	private P sampleLinear(int i, float s) {
		return (P) Mathf.lerp(points.get(i), points.get(i+1), s);
	}
	
	public float length() {
		return length;
	}
	
	public int count() { return points.size(); }
	
	public P get(int i) {
		return points.get(i);
	}
	
	public P first() { return points.get(0); }
	public P last() { return points.get(points.size()-1); }
	
	private ArrayList<P> step(ArrayList<P> src, float weight) {
		ArrayList<P> dst = new ArrayList<P>(src.size());
		
		int start = 0;
		if (smoothLength > 0) {
			start = src.size() - smoothLength;
			if (start < 0) 
				start = 0;
			for (int i = 0; i < start; i++)
				dst.add(src.get(i));
		}
		
		for (int i = start; i < src.size(); i++) {
			P p = src.get(i);
			
			if (i == start || i == src.size()-1) {
				dst.add(p.copy());
				continue;
			}
			
			P l = src.get(i-1);
			P r = src.get(i+1);
			
			P m = (P) Mathf.lerp(l,r,0.5f); // midpoint

			/*float pg = Mathf.magnitude(gradientDiscrete(i));
			pg /= 10;
			float gm = 1f/pg;
			gm = Mathf.clamp(0,1,gm);*/
			
			dst.add((P) Mathf.lerp(p,m,weight));
		}
		
		return dst;
	}
	
	public Curve<P> smoothed(float weight, int steps, boolean quadratic) {
		ArrayList<P> back = points;
		
		for (int iteration = 0; iteration < steps; iteration++) {
			back = step(back, weight);
			if (quadratic)
				step(back, -weight);
		}
		
		return new Curve<P>(back);
	}
	public Curve<P> smoothed(float weight, int steps) {
		return smoothed(weight, steps, true);
	}
	
	public Curve<P> resampled(int samples) {
		if (samples < 2)
			return null;
		
		ArrayList<P> pts = new ArrayList<P>();
		for (int i = 0; i < samples; i++)
			pts.add(sample(length*i/(samples-1)));
			
		return new Curve<P>(pts);
	}
	public Curve<P> resampled() {
		return resampled(count());
	}

	public Curve<P> resampledSmart(int samples) {
		ArrayList<P> pts = new ArrayList<P>(samples);
		pts.add(get(0).copy());
		
		for (int i = 1; i < samples-1; i++) {
			float f0 = length() * (i+0) / (samples-1f);
			float f1 = length() * (i+1) / (samples-1f);
			
			float fms = -1;
			float fmv = Float.NEGATIVE_INFINITY;
			
			for (float f = f0; f < f1; f += (f1-f0)/10) {
				float g = Mathf.magnitude(gradient(f));
				if (fms < 0 || g < fmv) {
					fms = f;
					fmv = g;
				}
			}
			
			pts.add(sample(fms));
		}
		
		pts.add(get(count()-1).copy());

		return new Curve<P>(pts);
	}
	
	public Curve<P> downsampled(int samples) {
		if (samples >= points.size())
			return new Curve<P>(this);
		return resampledSmart(samples);
	}
	
	public void clear() {
		length = 0;
		points.clear();
	}
	
	public Iterator<P> iterator() {
		return points.iterator();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(20);
		sb.append("C(count=");
		sb.append(count());
		sb.append(", length=");
		sb.append(length());
		sb.append(")");
		return sb.toString();
	}
}
