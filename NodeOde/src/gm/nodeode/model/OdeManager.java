package gm.nodeode.model;

import gm.nodeode.math.graph.Graph;

import java.util.Collection;
import java.util.HashMap;

public class OdeManager extends OdeAccess {

	private final Graph connectivity;
	private final HashMap<String, Visode> vertices;

	public OdeManager() {
		this(new HashMap<String, Visode>(), new Graph());
	}
	
	public OdeManager(OdeAccess access) {
		this();
		
		for (String v : access.getOdes()) {
			vertices.put(v, access.find(v));
		}
		for (String v : access.getOdes()) {
			for (String p : access.findParents(v))
				addParent(v, p);
		}
	}

	public OdeManager(HashMap<String, Visode> vertices, Graph connectivity) {
		this.connectivity = connectivity;
		this.vertices = vertices;
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
