package gm.nodeode.math.geom;


public class CubicHermite implements Cubic {
	public Pt sample(Pt p0, Pt t0, Pt p1, Pt t1, float s) {
		float t  = s;
		float t2 = s*s;
		float t3 = s*s*s;
		
		return Pt.P(0,0,0)
				.add(2*t3 - 3*t2 + 1, p0)
				.add(t3 - 2*t2 + t,   t0)
				.add(-2*t3 + 3*t2,    p1)
				.add(t3 - t2,         t1);
	}

}
