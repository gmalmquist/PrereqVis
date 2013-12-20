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
	
	public synchronized void addVertex(String vertex) {
		vertices.add(vertex);
	}
	
	public synchronized void addEdge(String tail, String head) {
		if (!linksTailHead.containsKey(tail)) linksTailHead.put(tail, new LinkedList<String>());
		if (!linksHeadTail.containsKey(head)) linksHeadTail.put(head, new LinkedList<String>());
		
		linksTailHead.get(tail).add(head);
		linksHeadTail.get(head).add(tail);
	}
	
	public synchronized void removeEdge(String tail, String head) {
		if (linksTailHead.containsKey(tail)) linksTailHead.get(tail).remove(head);
		if (linksHeadTail.containsKey(head)) linksHeadTail.get(head).remove(tail);
	}
	
	public void addEdge(Edge e) {
		addEdge(e.tail, e.head);
	}
	public void removeEdge(Edge e) {
		removeEdge(e.tail, e.head);
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
}
