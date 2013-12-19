package gm.nodeode.math;

import java.util.HashMap;
import java.util.Set;

public class PtField<T> {
	private HashMap<T, Pt> field;
	
	public PtField() {
		field = new HashMap<T, Pt>();
	}
	
	public void set(T t, Pt p) {
		field.put(t, p.d());
	}
	
	public Pt get(T t) {
		if (field.containsKey(t))
			return field.get(t).d();
		return Pt.P(0,0);
	}
	
	public void add(T t, Pt p) {
		if (field.containsKey(t))
			field.get(t).add(p);
		else
			set(t, p);
	}
	public void mul(T t, float s) {
		if (field.containsKey(t))
			field.get(t).mul(s);
		// otherwise do nothing because
		// N*0 = 0
	}
	
	public Set<T> keySet() {
		return field.keySet();
	}
}
