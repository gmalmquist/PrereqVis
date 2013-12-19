package gm.nodeode.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OdeManager extends OdeAccess {

	private final ConcurrentHashMap<String, List<String>> links;
	private final ConcurrentHashMap<String, Visode> odes;

	public OdeManager() {
		links = new ConcurrentHashMap<String, List<String>>();
		odes = new ConcurrentHashMap<String, Visode>();
	}
	
	public void clear() {
		links.clear();
		odes.clear();
	}
	
	@Override
	public Visode find(String id) {
		if (!odes.containsKey(id)) {
			return null;
		}
		return odes.get(id);
	}

	@Override
	public void register(Visode ode) {
		odes.put(ode.getUID(), ode);
	}

	@Override
	public void remove(Visode ode) {
		odes.remove(ode.getUID());
	}

	@Override
	public void addParent(String ode, String parent) {
		if (!links.containsKey(ode))
			links.put(ode, new LinkedList<String>());
		
		links.get(ode).add(parent);
	}
	
	@Override
	public Iterable<String> findParents(String ode) {
		if (!links.containsKey(ode)) {
			return new LinkedList<String>();
		}
		
		return links.get(ode);
	}

	@Override
	public boolean hasParents(String ode) {
		return links.containsKey(ode) && links.get(ode).size() > 0;
	}

	@Override
	public boolean hasChildren(String ode) {
		for (String other : odes.keySet()) {
			if (!hasParents(other)) continue;
			if (links.get(other).contains(ode))
				return true;
		}
		return false;
	}

	@Override
	public Collection<String> getOdes() {
		return odes.keySet();
	}

}
