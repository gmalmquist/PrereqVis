package gm.nodeode.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OdeManager extends OdeAccess {

	private final ConcurrentHashMap<String, List<String>> parents;
	private final ConcurrentHashMap<String, List<String>> children;
	private final ConcurrentHashMap<String, Visode> vertices;

	public OdeManager() {
		parents = new ConcurrentHashMap<String, List<String>>();
		children = new ConcurrentHashMap<String, List<String>>();
		vertices = new ConcurrentHashMap<String, Visode>();
	}
	
	public void clear() {
		parents.clear();
		vertices.clear();
	}
	
	@Override
	public Visode find(String id) {
		if (!vertices.containsKey(id)) {
			return null;
		}
		return vertices.get(id);
	}

	@Override
	public void register(Visode ode) {
		vertices.put(ode.getUID(), ode);
	}

	@Override
	public void remove(Visode ode) {
		vertices.remove(ode.getUID());
	}

	@Override
	public void addParent(String ode, String parent) {
		if (!parents.containsKey(ode))
			parents.put(ode, new LinkedList<String>());
		if (!children.containsKey(parent))
			children.put(parent, new LinkedList<String>());
		
		parents.get(ode).add(parent);
		children.get(parent).add(ode);
	}
	
	@Override
	public Iterable<String> findParents(String ode) {
		if (!parents.containsKey(ode)) {
			return new LinkedList<String>();
		}
		
		return parents.get(ode);
	}

	@Override
	public Iterable<String> findChildren(String ode) {
		if (children.containsKey(ode))
			return children.get(ode);
		return new LinkedList<String>();
	}

	@Override
	public boolean hasParents(String ode) {
		return parents.containsKey(ode) && parents.get(ode).size() > 0;
	}

	@Override
	public boolean hasChildren(String ode) {
		for (String other : vertices.keySet()) {
			if (!hasParents(other)) continue;
			if (parents.get(other).contains(ode))
				return true;
		}
		return false;
	}

	@Override
	public Collection<String> getOdes() {
		return vertices.keySet();
	}

}
