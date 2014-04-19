package gm.nodeode.model;

import gm.nodeode.math.graph.Graph;
import gm.nodeode.math.graph.UnionFind;

import java.util.Collection;
import java.util.List;

/**
 * Abstract interface for accessing both graph connectivity and visual node data.
 * @author Garrett
 *
 */
public abstract class OdeAccess {
	public abstract Visode find(String id);
	public abstract void register(Visode ode);
	public abstract void remove(Visode ode);
	public abstract void addParent(String ode, String parent);
	public abstract void clear();

	public abstract Collection<String> getOdes();
	public abstract Iterable<String> findParents(String ode);
	public abstract Iterable<String> findChildren(String ode);
	public abstract Iterable<Visode> getVisodes();
	
	public abstract boolean hasParents(String ode);
	public abstract boolean hasChildren(String ode);
	
	public List<List<String>> findDisjointGroups() {
		UnionFind<String> uf = new UnionFind<String>();
		for (String s : getOdes())
			for (String p : findParents(s))
				uf.union(s, p);
		return uf.discreteGroups(getOdes());
	}
	
	public boolean hasParent(String ode, String parent) {
		for (String p : findParents(ode))
			if (p.equals(parent))
				return true;
		return false;
	}
	
	public boolean hasChild(String ode, String child) {
		return hasParent(child, ode);
	}
	public abstract Graph copyGraph();
}
