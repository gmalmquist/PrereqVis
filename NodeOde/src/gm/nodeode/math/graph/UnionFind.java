package gm.nodeode.math.graph;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Extremely useful union-find data structure
 * @author Garrett
 *
 * @param <T>
 */
public class UnionFind<T> {
	private HashMap<T, T> parents;
	
	public UnionFind() {
		this.parents = new HashMap<T, T>();
	}
	
	public T find(T t) {
		if (parents.containsKey(t)) {
			T parent = find(parents.get(t));
			parents.put(t, parent); // collapse the chain
			return parent;
		}
		return t;
	}
	
	public void union(T one, T two) {
		one = find(one);
		two = find(two);
		
		if (one.equals(two))
			return; // already joined

		parents.put(one, two);
	}
	
	public void clear() {
		parents.clear();
	}
	
	public List<List<T>> discreteGroups() {
		return discreteGroups(parents.keySet());
	}
	public List<List<T>> discreteGroups(Iterable<T> items) {
		final HashMap<T, List<T>> groups = new HashMap<T, List<T>>();
		
		for (T item : items) {
			T parent = find(item);
			if (!groups.containsKey(parent))
				groups.put(parent, new LinkedList<T>());
			
			groups.get(parent).add(item);
		}
		
		List<T> keys = new LinkedList<T>();
		keys.addAll(groups.keySet());
		Collections.sort(keys, new Comparator<T>() {
			public int compare(T a, T b) {
				return groups.get(a).size() - groups.get(b).size();
			}
		});
		
		List<List<T>> results = new LinkedList<List<T>>();
		for (T s : keys)
			results.add(groups.get(s));
		
		return results;
	}
}
