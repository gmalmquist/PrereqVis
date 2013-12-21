package gm.nodeode.math.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Graph that uses strings (must be unique!) for vertices
 * @author Garrett
 *
 */
public class Graph {
	
	private List<String> vertices;
	private HashMap<String, List<String>> linksTailHead;
	private HashMap<String, List<String>> linksHeadTail;
	
	public Graph() {
		this.vertices = new LinkedList<String>();
		linksTailHead = new HashMap<String, List<String>>();
		linksHeadTail = new HashMap<String, List<String>>();
	}
	
	public synchronized void clear() {
		vertices.clear();
		linksTailHead.clear();
		linksHeadTail.clear();
	}
	
	public synchronized void addVertex(String vertex) {
		if (!vertices.contains(vertex))
			vertices.add(vertex);
	}
	
	public synchronized void addEdge(String tail, String head) {
		if (!linksTailHead.containsKey(tail)) linksTailHead.put(tail, new LinkedList<String>());
		if (!linksHeadTail.containsKey(head)) linksHeadTail.put(head, new LinkedList<String>());
		
		List<String> lth = linksTailHead.get(tail);
		List<String> lht = linksHeadTail.get(head);
		
		if (!lth.contains(head)) lth.add(head);
		if (!lht.contains(tail)) lht.add(tail);
	}
	
	public synchronized void removeEdge(String tail, String head) {
		if (linksTailHead.containsKey(tail)) linksTailHead.get(tail).remove(head);
		if (linksHeadTail.containsKey(head)) linksHeadTail.get(head).remove(tail);
	}
	
	/**
	 * Merges b into a
	 * @param a
	 * @param b
	 */
	public synchronized void mergeVertices(String a, String b) {
		for (String out : getOutgoingVertices(b)) {
			addEdge(a, out);
		}
		for (String inn : getIncomingVertices(b)) {
			addEdge(inn, a);
		}
		
		removeVertex(b);
	}
	
	public int vertexCount() {
		return vertices.size();
	}
	
	public void addEdge(Edge e) {
		addEdge(e.tail, e.head);
	}
	public void removeEdge(Edge e) {
		removeEdge(e.tail, e.head);
	}
	
	public synchronized boolean hasEdge(String tail, String head) {
		if (!linksTailHead.containsKey(tail))
			return false;
		return linksTailHead.get(tail).contains(head);
	}
	public synchronized boolean hasEdge(Edge e) {
		return hasEdge(e.tail, e.head);
	}
	
	public synchronized void removeVertex(String vertex) {
		vertices.remove(vertex);
		
		if (linksTailHead.containsKey(vertex)) {
			for (String b : linksTailHead.get(vertex)) {
				linksHeadTail.get(b).remove(vertex);
			}
			linksTailHead.remove(vertex);
		}
		
		if (linksHeadTail.containsKey(vertex)) {
			for (String a : linksHeadTail.get(vertex)) {
				linksTailHead.get(a).remove(vertex);
			}
			linksHeadTail.remove(vertex);
		}
	}
	
	public synchronized Collection<String> getOutgoingVertices(String vertex) {
		if (linksTailHead.containsKey(vertex))
			return linksTailHead.get(vertex);
		return new LinkedList<String>();
	}
	public synchronized Collection<String> getIncomingVertices(String vertex) {
		if (linksHeadTail.containsKey(vertex))
			return linksHeadTail.get(vertex);
		return new LinkedList<String>();
	}
	
	public synchronized Collection<Edge> getEdges(String vertex) {
		List<Edge> edges = new LinkedList<Edge>();
		for (String s : getOutgoingVertices(vertex))
			edges.add(new Edge(vertex, s));
		for (String s : getIncomingVertices(vertex))
			edges.add(new Edge(s, vertex));
		
		return edges;
	}
	
	public synchronized List<Graph> getConnectedSubgraphs() {
		List<Graph> graphs = new LinkedList<Graph>();
		
		UnionFind<String> uf = new UnionFind<String>();
		for (String v : vertices) {
			for (String o : getOutgoingVertices(v)) {
				uf.union(v, o);
			}
		}
		
		List<List<String>> subgroups = uf.discreteGroups(vertices);
		
		for (List<String> subverts : subgroups) {
			Graph sub = new Graph();
			
			for (String v : subverts) {
				sub.addVertex(v);
				for (String o : getOutgoingVertices(v))
					sub.addEdge(v, o);
			}
			
			graphs.add(sub);
		}
		
		return graphs;
	}
	
	public boolean isConnected() {
		return getConnectedSubgraphs().size() == 1;
	}
	
	public synchronized Collection<String> getVertices() {
		return vertices;
	}
	
	private long uniqueID = 0;
	public synchronized String addUniqueVertex() {
		String id = null;
		do {
			id = String.valueOf(uniqueID++);
		} while (vertices.contains(id));
		addVertex(id);
		return id;
	}
}
