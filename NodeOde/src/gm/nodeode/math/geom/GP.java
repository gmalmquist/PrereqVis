package gm.nodeode.math.geom;

/**
 * Generic Point interface, very useful for widgetry.
 * @author Garrett
 *
 */
public interface GP {
	public float x();
	public float y();
	public float z();
	
	public GP x(float x);
	public GP y(float y);
	public GP z(float z);
	
	public int ix();
	public int iy();
	public int iz();
	
	public Pt toPt();
	public void fromPt(Pt P);
	
	public GP set(GP o);
	public GP add(float s, GP o);
	public GP add(GP o);
	public GP copy();
}
