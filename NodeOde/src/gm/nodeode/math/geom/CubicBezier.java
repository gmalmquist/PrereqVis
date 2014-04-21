package gm.nodeode.math.geom;


public class CubicBezier implements Cubic {
	
	private Pt bezier(Pt p0, Pt p1, float s) {
		return p0.d().lerp(p1, s);
	}
	
	private Pt bezier(Pt p0, Pt p1, Pt p2, float s) {
		return bezier(bezier(p0, p1, s), bezier(p1, p2, s), s);
	}
	
	private Pt bezier(Pt p0, Pt p1, Pt p2, Pt p3, float s) {
		return bezier(bezier(p0, p1, p2, s), bezier(p1, p2, p3, s), s);
	}
	
	public Pt sample(Pt p0, Pt p1, Pt p2, Pt p3, float s) {
		return bezier(p0, p1, p2, p3, s);
	}

}
