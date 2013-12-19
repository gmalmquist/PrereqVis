package gm.nodeode.math;

/**
 * Abstract curve interface for use in widgets, drawings, animations, etc. It's highly
 * abstract, which is why it's in math, and not GUI or something different.
 * @author Garrett
 *
 * @param <P>
 */
public interface ICurve<P extends Interpolable<P>> extends Iterable<P> {
	
	/**
	 * Shift the entire curve by the given amount
	 * @param by
	 */
	public void shift(P by);
	
	/**
	 * Return the point along this curve at the given "time"
	 * @param t - should be a float in [0, length()]
	 * @return
	 */
	public P sample(float t);
	
	/**
	 * Length of this curve
	 * @return
	 */
	public float length();
	
	/**
	 * Returns the first derivative at the given "time".
	 * @param t - should be a float in [0, length()]
	 * @return
	 */
	public P gradient(float t);
	
}
