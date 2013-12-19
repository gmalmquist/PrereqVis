package gm.nodeode.math;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HermiteLoop implements ICurve<Pt> {

	private List<Pt> points;
	private List<Pt> velocities;
	
	private Pt offset;
	private float length;
	
	public HermiteLoop() {
		this.points = new ArrayList<Pt>(3);
		this.velocities = new ArrayList<Pt>(3);
		
		this.offset = new Pt(0,0);
		this.length = 0;
	}
	
	public void add(Pt p, Pt v) {
		points.add(p.copy().sub(offset));
		velocities.add(v.copy().sub(offset));
		calcLength();
	}
	
	public void add(Pt[] pts, Pt[] vs) {
		if (vs.length != pts.length)
			return;
		for (int i = 0; i < pts.length; i++) {
			points.add(pts[i].copy().sub(offset));
			velocities.add(vs[i].copy().sub(offset));
		}
		calcLength();
	}
	
	public void clear() {
		points.clear();
		velocities.clear();
		length = 0;
		offset.set(0,0);
	}
	
	private void calcLength() {
		if (points.size() < 2)
			return;
		
		float len = 0;
		
		for (int i = 0; i < points.size(); i++) {
			len += points.get(i).distance(points.get((i+1)%points.size()));
		}
		
		this.length = len;
	}
	
	@Override
	public Iterator<Pt> iterator() {
		return new Iterator<Pt>() {
			int index = 0;
			@Override
			public boolean hasNext() {
				return index <= points.size();
			}
			@Override
			public Pt next() {
				if (hasNext())
					return points.get((index++)%points.size());
				return null;
			}
			@Override
			public void remove() { }
		};
	}

	@Override
	public void shift(Pt by) {
		offset.add(by);
	}
	
	private Pair<Integer, Float> index(float time) {
		if (length <= 0) 
			return new Pair<Integer, Float>(0,0f);
		
		while (time < 0) time += length;
		while (time > length) time -= length;
		
		int N = points.size();
		for (int i = 0; i < N && time >= 0; i++) {
			Pt s0 = points.get(i);
			Pt s1 = points.get((i+1)%N);
			
			float dist = s0.distance(s1);
			if (dist == 0) continue;
			
			if (time < dist)
				return new Pair<Integer, Float>(i, time/dist);
			
			time -= dist;
		}
		
		return new Pair<Integer, Float>(0, 0f);
	}

	private Pt sample(int i, float t) {
		int N = points.size();
		
		Pt s0 = points.get(i);
		Pt v0 = velocities.get(i);
		Pt s1 = points.get((i+1)%N);
		Pt v1 = velocities.get((i+1)%N);
		
		return offset.copy()
				.add(2*t*t*t - 3*t*t +1, s0)
				.add(t*t*t - 2*t*t + t, v0)
				.add(-2*t*t*t + 3*t*t, s1)
				.add(t*t*t - t*t, v1);
	}
	
	@Override
	public Pt sample(float t) {
		Pair<Integer, Float> pos = index(t);
		return sample(pos.a, pos.b);
	}

	@Override
	public float length() {
		return length;
	}

	@Override
	public Pt gradient(float t) {
		return sample(t+0.1f).sub(sample(t-0.1f)).mul(5f);
	}
	
	
}
