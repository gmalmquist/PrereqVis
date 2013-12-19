package gm.nodeode.model;

import java.util.Collection;
import java.util.List;


public abstract class OdeAccess {
	public abstract Visode find(String id);
	public abstract void register(Visode ode);
	public abstract void remove(Visode ode);
	public abstract void addParent(String ode, String parent);
	public abstract void clear();

	public abstract Collection<String> getOdes();
	public abstract Iterable<String> findParents(String ode);
	public abstract Iterable<String> findChildren(String ode);
	
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
}
