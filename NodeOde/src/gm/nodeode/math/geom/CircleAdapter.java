package gm.nodeode.math.geom;

/**
 * Adapter for reading circle geometry from an object
 * @author Garrett
 *
 * @param <T>
 */
public interface CircleAdapter<T> {
	// getters
	public Pt position(T t);
	public float radius(T t);
	public float mass(T t);

	// setters
	public void moveBy(T t, Pt v);
	public void moveTo(T t, Pt p);
}
