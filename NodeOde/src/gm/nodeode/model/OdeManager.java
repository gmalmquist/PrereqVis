package gm.nodeode.model;

import gm.nodeode.math.graph.Graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OdeManager extends OdeAccess {

	private final Graph connectivity;
	private final ConcurrentHashMap<String, Visode> vertices;

	public OdeManager() {
		connectivity = new Graph();
		vertices = new ConcurrentHashMap<String, Visode>();
	}
	
	public void clear() {
		vertices.clear();
		connectivity.clear();
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
		connectivity.addEdge(ode, parent);
	}
	
	@Override
	public Iterable<String> findParents(String ode) {
		return connectivity.getOutgoingVertices(ode);
	}

	@Override
	public Iterable<String> findChildren(String ode) {
		return connectivity.getIncomingVertices(ode);
	}

	@Override
	public boolean hasParents(String ode) {
		return connectivity.getOutgoingVertices(ode).size() > 0;
	}

	@Override
	public boolean hasChildren(String ode) {
		return connectivity.getIncomingVertices(ode).size() > 0;
	}

	@Override
	public Collection<String> getOdes() {
		return vertices.keySet();
	}

	@Override
	public Graph copyGraph() {
		return new Graph(connectivity);
	}

}
