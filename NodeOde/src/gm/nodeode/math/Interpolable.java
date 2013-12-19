package gm.nodeode.math;

/**
 * This is effectively an abstract point class, though it could be used for
 * other things (like images). Anything which can be interpolated between is valid
 * for this, which is great, because it means the Curve class can use the same code
 * to smooth very different kinds of objects.
 * @author Garrett
 *
 * @param <T>
 */
public interface Interpolable<T extends Interpolable<?>> {

	/**
	 * Returns distance between this abstract point and that one
	 * @param other
	 * @return
	 */
	public float distance(T other);

	/**
	 * Adds the other point, weighted by the scalar s.
	 * @param s
	 * @param other
	 * @return
	 */
	public T add(float s, T other);
	/**
	 * Returns a copy of this point.
	 * @return
	 */
	public T copy();
	
	/**
	 * Returns this point scaled by the given float.
	 * @param s
	 * @return
	 */
	public T mul(float s);
}
