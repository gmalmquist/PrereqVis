package gm.nodeode.model;

import gm.nodeode.math.graph.Graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Stores connectivity data and a map of node ID's to visual metadata (position, name, radius, etc)
 * @author Garrett
 *
 */
public class OdeManager extends OdeAccess {

	private final Graph connectivity;
	private final HashMap<String, Visode> vertices;

	private static HashMap<String, Visode> mapify(Visode[] nodes) {
		HashMap<String, Visode> map = new HashMap<String, Visode>();
		for (Visode n : nodes)
			map.put(n.getUID(), n);
		return map;
	}
	
	public OdeManager() {
		this(new HashMap<String, Visode>(), new Graph());
	}
	
	public OdeManager(Visode[] vertices, Graph connectivity) {
		this(mapify(vertices), connectivity);
	}
	
	public OdeManager(OdeAccess access) {
		this();
		
		for (String v : access.getOdes()) {
			vertices.put(v, access.find(v));
			connectivity.addVertex(v);
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
		connectivity.addVertex(ode.getUID());
	}

	@Override
	public void remove(Visode ode) {
		vertices.remove(ode.getUID());
		connectivity.removeVertex(ode.getUID());
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

	@Override
	public Iterable<Visode> getVisodes() {
		List<Visode> vs = new LinkedList<Visode>();
		for (String s : getOdes()) {
			vs.add(find(s));
		}
		return vs;
	}

}
