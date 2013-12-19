package gm.nodeode.math;

/**
 * Holds a pair of generic values.
 * 
 * @author Garrett
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A,B> {
	public A a;
	public B b;
	
	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	public Pair() {}
}
