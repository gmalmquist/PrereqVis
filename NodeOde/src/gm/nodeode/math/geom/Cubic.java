package gm.nodeode.math.geom;


public interface Cubic {
	public Pt sample(Pt a, Pt b, Pt c, Pt d, float s);
	
	public static final Cubic HERMITE = new CubicHermite();
	public static final Cubic BEZIER = new CubicBezier();
}
