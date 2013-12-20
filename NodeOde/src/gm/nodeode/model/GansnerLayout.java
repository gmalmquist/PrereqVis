package gm.nodeode.model;

import gm.nodeode.math.TimeKeeper;
import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Edge;
import gm.nodeode.math.graph.Graph;
import gm.nodeode.math.graph.UnionFind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Acquired from http://www.graphviz.org/Documentation/TSE93.pdf, with some modifications
 * which I can do because we have additional domain knowledge
 * @author Garrett
 *
 */

public class GansnerLayout extends OdeLayout {
	
	 // 24 = magic number given in paper for decent results
	private static final int MAX_ORDERING_ITERATIONS = 24;
	private static final int MAX_TRANSPOSITIONS = 100;

	public GansnerLayout(OdeAccess db) {
		super(db);
	}

	@Override
	public void doLayout() {
		// Operation order identical to paper
		rank();
		ordering();
		position();
		makeSplines();
		
		// push results
		virtualToDB();
		
		System.out.println("Modified Gansner Layout complete.");
	}

	private void virtualToDB() {
		HashMap<String, String> names = new HashMap<String, String>();
		for (String v : virtual.getVertices()) {
			Visode o = db.find(v);
			String name = o == null ? v : String.valueOf(((OdeNode)o).getDisplayName());
			names.put(v, name);
		}
		
		db.clear();
		for (String v : virtual.getVertices()) {
			Visode ode = new OdeNode(v, names.get(v));
			ode.setCenter(positions.get(v));
			db.register(ode);
			for (String o : virtual.getOutgoingVertices(v))
				db.addParent(v, o);
		}
	}
	
	private int step = 0;
	private int maxrank = Integer.MIN_VALUE, minrank = Integer.MAX_VALUE;
	@Override
	public void doLayoutStep() {
		switch (step++) {
		case 0: rank(); break;
		case 1: ordering(); break;
		case 2: position(); break;
		case 3: makeSplines(); break;
		}
	}
	
	private Graph virtual;
	private HashMap<String, Integer> ranks;
	
	/**
	 * This rank works completely differently than the method described by the paper,
	 * because I'm cheating by using domain knowledge (like the fact that the graph
	 * is guaranteed to be acyclic)
	 */
	private void rank() {
		ranks = new HashMap<String, Integer>();
		
		// STEP ONE: Rank bottom-up, root nodes at 0 and leaves and big numbers
		LinkedList<String> frontier = new LinkedList<String>();
		for (String s : vertices()) {
			if (!db.hasParents(s))
				frontier.add(s);
		}

		int maxd = 0;
		while (!frontier.isEmpty()) {
			String f = frontier.pop();
			if (ranks.containsKey(f))
				continue;
			
			boolean rankValid = true;
			int depth = 0;
			for (String p : db.findParents(f)) {
				if (ranks.containsKey(p)) {
					depth = Math.max(depth, ranks.get(p)+1);
				} else { // oh no!
					rankValid = false;
					break;
				}
			}
			if (rankValid) {
				if (depth > maxd)
					maxd = depth;
				ranks.put(f, depth);
			}
			
			for (String s : db.findChildren(f)) {
				frontier.add(s);
			}
			
			if (!rankValid)
				frontier.add(f);
		}
		
		// Reverse order so root nodes are bigger numbers
		for (String v : vertices()) {
			ranks.put(v, maxd - ranks.get(v));
		}
		maxrank = maxd;
		minrank = 0;

		// Move up dangling root nodes
		for (String v : vertices()) {
			if (!db.hasParents(v)) {
				int minc = ranks.get(v);
				int maxx = Integer.MIN_VALUE;
				for (String c : db.findChildren(v)) {
					maxx = Math.max(maxx, ranks.get(c)+1);
				}
				if (maxx > Integer.MIN_VALUE)
					minc = maxx;
				ranks.put(v, minc);
			}
		}
		
		// Rank nodes with no children a half-step lower
		// Widen ranks
		HashMap<Integer, List<String>> rankings = new HashMap<Integer, List<String>>();
		for (String v : vertices()) {
			int r = ranks.get(v)*2;
			if (!db.hasChildren(v))
				r++;

			if (!rankings.containsKey(r))
				rankings.put(r, new LinkedList<String>());
			
			rankings.get(r).add(v);
		}
		int ri = 0;
		for (int i = 0; i <= maxrank*2+1; i++) {
			if (!rankings.containsKey(i))
				continue;
			for (String v : rankings.get(i))
				ranks.put(v, ri);
			ri++;
		}
		maxrank = ri-1;
		
//		// verification
//		for (String v : vertices()) {
//			int parmax = maxrank;
//			for (String p : db.findParents(v)) {
//				parmax = Math.min(parmax, ranks.get(p)-1);
//			}
//			// higher up is lower rank now, so
//			// arank should be <= parmax.
//			// 2, 3 -> 4, 6 -> 3, 7
//			int arank = ranks.get(v);
//			if (arank > parmax) {
//				throw new RuntimeException("Ranking error! " + v + ", " + arank + ", should be: " + parmax + "!");
//			}
//		}
		
		// Construct graph with virtual nodes
		virtual = new Graph();
		HashMap<String, Boolean> fakes = new HashMap<String, Boolean>();
		
		for (String v : db.getOdes()) {
			virtual.addVertex(v);
		}
		for (String v : db.getOdes()) {
			for (String p : db.findParents(v)) {
				int rv = rank(v);
				int rp = rank(p);
				
				if (Math.abs(rv-rp) <= 1) {
					virtual.addEdge(v, p);
				} else { // Need intermediates!
					// direction of travel
					int s = Mathf.sign(rp - rv);
					
					String last = v;
					for (int r = rv+s; r != rp; r += s) {
						String intVertex = virtualID(v, p, r);
						fakes.put(intVertex, true);
						ranks.put(intVertex, r);
						virtual.addEdge(last, intVertex);
						last = intVertex;
					}
					virtual.addEdge(last, p);
				}
			}
		}
		
//		List<String> overts = new LinkedList<String>();
//		overts.addAll(virtual.getVertices());
//		for (int i = maxrank; i >= minrank; i--) {
//			
//			UnionFind<String> destinations = new UnionFind<String>();
//			List<String> intermediates = new LinkedList<String>();
//			
//			for (String v : overts) {
//				if (ranks.get(v) != i) continue;
//				if (!fakes.containsKey(v) || !fakes.get(v)) continue;
//				
//				intermediates.add(v);
//				boolean only = true;
//				for (String p : virtual.getOutgoingVertices(v)) {
//					if (!only) {
//						throw new RuntimeException("Virtual node cannot have multiple outgoing vertices!");
//					}
//					destinations.union(p, v);
//					only = false;
//				}
//			}
//			
//			for (List<String> group : destinations.discreteGroups()) {
//				String last = null;
//				for (String v : group) {
//					if (!intermediates.contains(v))
//						continue;
//					if (last != null) {
//						virtual.mergeVertices(last, v);
//					} else {
//						last = v;
//					}
//				}
//			}
//		}
		
	}
	
	private HashMap<String, String> virtIDs = new HashMap<String, String>();
	private String virtualID(String v, String p, int r) {
		StringBuffer sb = new StringBuffer(v.length() + p.length() + 5);
		sb.append(p);
		sb.append(":");
		sb.append(r);
		String key = sb.toString();
		
		String result = null;
		if (virtIDs.containsKey(key)) {
			result = virtIDs.get(key);
			virtual.addVertex(result);
		} else {
			result = virtual.addUniqueVertex();
			virtIDs.put(key, result);
		}
		
		return result;
		
	}
	private int rank(String v) {
		return ranks.get(v);
	}
	
	private Order orders;
	private void ordering() {
		Order best = initialOrdering();
		
		int maxIterations = MAX_ORDERING_ITERATIONS;

		Order order = copy(best);
		for (int i = 0; i < maxIterations; i++) {			
			System.out.println("Ordering Iteration " + i);
			
			System.out.println("\tMedian sorting");
			wmedian(order, i);
			
			System.out.println("\tTransposing");
			transpose(order);
			
			System.out.println("\tCross testing");
			int newOrder = crossingCount(order);
			int oldOrder = crossingCount(best);
//			for (Integer R : order.keySet())
//				System.out.println("\t\t [" + R + "] " + crossingCount(order, R) + " < " + crossingCount(best, R) + " ?");
				
			System.out.println("\t\t" + newOrder + " < " + oldOrder + " ?");
			
			// == DEBUG ==
			this.orders = order;
			position();
			virtualToDB();
			// ===========
			
			if (newOrder < oldOrder) {
				System.out.println("\t\tImprovement made!");
				best = order;
			} else {
				// Pretty sure further improvements impossible now?
				System.out.println("\t\tNo improvement made.");
//				break;
			}
		}
		
		this.orders = best;
	}
	private void wmedian(Order order, int iter) {
		int r0 = minrank;
		int r1 = maxrank;
		int rd = 1;
		if (iter % 2 == 1) {
			r0 = maxrank;
			r1 = minrank;
			rd = -1;
		}
		
		for (int r = r0; r*rd <= r1*rd; r += rd) {
			System.out.print(" " + r + ", ");
			List<String> rord = order.get(r);
			final HashMap<String, Float> medians = new HashMap<String, Float>();
			
			for (String v : rord) {
				float med = medianValue(order, v, r-rd);
				if (Float.isNaN(med) || Float.isInfinite(med)) {
					System.err.println("Warning: garbage data in median sorter");
					med = 0;
				}
				medians.put(v, med);
			}
			// sort(order[r], median)
			Collections.sort(rord, new Comparator<String>() {
				public int compare(String a, String b) {
					float ma = medians.get(a);
					float mb = medians.get(b);
					if (ma > mb) return 1;
					if (mb > ma) return -1;
					return 0;
				}
			});
			
			order.put(r, rord);
		}
		
	}
	private float[] adjPosition(Order order, String v, int adjRank) {
		if (!order.containsKey(adjRank))
			return new float[] {};
		
		List<String> adjacent = new LinkedList<String>();
		List<Float> poses = new LinkedList<Float>();
		
		int i = 0;
		for (String rank : order.get(adjRank)) {
			if (virtual.hasEdge(v, rank) || virtual.hasEdge(rank, v)) {
				adjacent.add(rank);
				poses.add((float) i);
			}
			i++;
		}

		float[] results = new float[poses.size()];
		int j = 0;
		for (float f : poses)
			results[j++] = f;
		
		return results;
	}
	private float medianValue(Order order, String v, int adjRank) {
		float[] P = adjPosition(order, v, adjRank);
		
		int m = P.length/2;
		
		if (P.length == 0)
			return -1.0f;
		else if (P.length % 2 == 1)
			return P[m];
		
		float left = P[m-1] - P[0];
		float right = P[P.length-1] - P[m];
		return (P[m-1]*right + P[m]*left) / (left + right);
		
	}
	private void transpose(Order order) {
		// Basically does an iterative bubble sort to iron out small crossings
		int maxIterations = MAX_TRANSPOSITIONS;
		boolean improved = true;
		while (improved && maxIterations-- > 0) {
			improved = false;
			for (int r = minrank; r <= maxrank; r++) {
				List<String> rankr = order.get(r);
				for (int i = 0; i < rankr.size()-1; i++) {
					String v = rankr.get(i);
					String w = rankr.get(i+1);
					
					int crossingVW = crossingCount(order, r) + crossingCount(order, r-1);
					
					// swap and see if there's any improvement
					rankr.set(i+0, w);
					rankr.set(i+1, v);
					int crossingWV = crossingCount(order, r) + crossingCount(order, r-1);
					
					if (crossingVW <= crossingWV) {
						// swap back
						rankr.set(i+0, v);
						rankr.set(i+1, w);
					} else {
						improved = true;
					}
				}
			}			
		}
		if (improved) System.out.println("Stopped by cap");
		else System.out.println("Improving ceased at i=" + (MAX_TRANSPOSITIONS - maxIterations));
	}
	private int crossingCount(Order order) {
		int crosses = 0;
		for (int r = minrank; r < maxrank; r++)
			crosses += crossingCount(order, r);
		return crosses;
	}
	private int crossingCount(Order order, int rank) {
		if (rank < minrank || rank > maxrank)
			return 0;
		if (rank == maxrank)
			rank--;
		
		LinkedList<Edge> edges = new LinkedList<Edge>();
		for (String v0 : order.get(rank)) {
			for (String v1 : order.get(rank+1)) {
				// We don't care about direction for this
				if (virtual.hasEdge(v0, v1) || virtual.hasEdge(v1, v0)) {
					edges.add(new Edge(v0, v1));
				}
			}
		}
		
		int crosses = 0;
		
		while (edges.size() > 0) {
			Edge A = edges.pop();
			
			int a0 = order.get(rank+0).indexOf(A.tail);
			int a1 = order.get(rank+1).indexOf(A.head);
			for (Edge B : edges) {
				int b0 = order.get(rank+0).indexOf(B.tail);
				int b1 = order.get(rank+1).indexOf(B.head);
				
				if (Mathf.sign(a0-b0) != Mathf.sign(a1-b1)) {
					crosses++;
				}
			}
		}
		
		return crosses;
	}
	private Order initialOrdering() {
		// Lazy breadth-first search of an arbitrary tree.
		Order orders = new Order();
		HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
		LinkedList<String> frontier = new LinkedList<String>();
		
//		// Lazy: just add the first vertex in the list
//		for (String s : virtual.getVertices()) {
//			frontier.add(s);
//			break;
//		}
		
		// Better (maybe): add the most-constrained vertex first.
		int mostEdges = 0;
		String best = null;
		final Hashtable<String, Integer> edgeCount = new Hashtable<String, Integer>();
		for (String s : virtual.getVertices()) {
			int e = virtual.getEdges(s).size();
			edgeCount.put(s,  e);
			if (best == null || e > mostEdges) {
				best = s;
				mostEdges = e;
			}
		}
		frontier.add(best);
		
		final Comparator<String> constraint = new Comparator<String>() {
			public int compare(String a, String b) {
				return edgeCount.get(a) - edgeCount.get(b);
			}
		};
		
		while (!frontier.isEmpty()) {
			String v = frontier.pop();
			if (visited.containsKey(v))
				continue;
			visited.put(v, true);
			int rank = rank(v);
			if (!orders.containsKey(rank))
				orders.put(rank, new ArrayList<String>());
			orders.get(rank).add(v);
			
			List<String> outs = new LinkedList<String>();
			List<String> inns = new LinkedList<String>();
			
			outs.addAll(virtual.getOutgoingVertices(v));
			inns.addAll(virtual.getIncomingVertices(v));
			
//			Collections.sort(outs, constraint);
//			Collections.sort(inns, constraint);
			
			List<String> alls = new LinkedList<String>();
			alls.addAll(outs);
			alls.addAll(inns);
			
			Collections.sort(alls, constraint);
			
			frontier.addAll(alls);
		}
		
		return orders;
	}
	private Order copy(Order order) {
		Order newWorldOrder = new Order();
		for (Integer key : order.keySet()) {
			List<String> ls = new LinkedList<String>();
			ls.addAll(order.get(key));
			newWorldOrder.put(key, ls);
		}
		return newWorldOrder;
	}

	private HashMap<String, Pt> positions;
	private void position() {
		positions = new HashMap<String, Pt>();
		
		gridPositioning();
	}
	private void position(String v, Pt p) {
		positions.put(v, p);
	}
	private void gridPositioning() {
		float[] widths = new float[maxrank-minrank+1];
		float height = 0;
		for (int i = minrank; i <= maxrank; i++) {
			List<String> verts = orders.get(i);
			
			float mr = 0;
			String last = null;
			for (String v : verts) {
				if (radius(v) > mr) mr = radius(v);
				
				widths[i-minrank] += radius(v);
				if (last != null) {
					widths[i-minrank] += mindist(last, v);
				}
				last = v;
			}
			
			height += mr*6f;
		}

		float width = Mathf.max(widths);
		Arrays.fill(widths, width);
		
		for (int i = 0; i < widths.length; i++) {
			List<String> verts = orders.get(i+minrank);
			if (verts == null) continue;
			int j = 0;
			for (String v : verts) {
				position(v, Pt.P(
						widths[i] * (j++)/(verts.size()),
						height * (i+1) / (widths.length+1)
				));
			}
		}
	}
	
	private void makeSplines() {
		
	}
	
	
	private float xsize(String v) {
		return radius(v);
	}
	private float ysize(String v) {
		return radius(v);
	}
	private float nodesep() {
		return 10;
	}
	private float ranksep() {
		return 30;
	}
	
	private float mindist(String a, String b) {
		return 1.5f * (radius(a) + radius(b));
	}
	
	private float weight(String a, String b) {
		if (db.hasParent(a, b) || db.hasParent(b, a)) return 1;
		return 0;
	}
	
	private Collection<String> vertices() {
		return db.getOdes();
	}
	
	private float radius(String s) {
		if (db.find(s) == null)
			return 0;
		return db.find(s).radius();
	}

	// Because Java doesn't have typedefs
	private class Order extends HashMap<Integer, List<String>> { }
	
}

