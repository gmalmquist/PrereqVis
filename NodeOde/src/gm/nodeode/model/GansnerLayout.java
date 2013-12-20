package gm.nodeode.model;

import gm.nodeode.math.TimeKeeper;
import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Edge;
import gm.nodeode.math.graph.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
	private static final int MAX_ORDERING_ITERATIONS = 2;
	private static final int MAX_TRANSPOSITIONS = 30;

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
		
//		This works fine.
//		// verification
//		for (String v : vertices()) {
//			int parmax = 0;
//			for (String p : db.findParents(v)) {
//				parmax = Math.max(parmax, ranks.get(p)+1);
//			}
//			if (parmax != ranks.get(v)) {
//				throw new RuntimeException("Ranking error! " + v + ", " + ranks.get(v) + ", should be: " + parmax + "!");
//			}
//		}
		
		for (String v : vertices()) {
			ranks.put(v, maxd - ranks.get(v));
		}
		
		maxrank = maxd;
		minrank = 0;
		
		// Widen ranks
		HashMap<Integer, List<String>> rankings = new HashMap<Integer, List<String>>();
		for (String v : vertices()) {
			int r = ranks.get(v)*2;
			if (!db.hasChildren(v))
				r--;

			if (!rankings.containsKey(r))
				rankings.put(r, new LinkedList<String>());
			
			rankings.get(r).add(v);
		}
		int ri = 0;
		for (int i = -1; i <= maxrank*2; i++) {
			if (!rankings.containsKey(i))
				continue;
			for (String v : rankings.get(i))
				ranks.put(v, ri);
			ri++;
		}
		maxrank = ri-1;
		
		// Construct graph with virtual nodes
		virtual = new Graph();
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
						String virt = virtual.addUniqueVertex();
						ranks.put(virt, r);
						virtual.addEdge(last, virt);
						last = virt;
					}
					virtual.addEdge(last, p);
				}
			}
		}
	}
	private String virtualID(String v, String p, int r) {
		StringBuffer sb = new StringBuffer(v.length() + p.length() + 5);
		sb.append("VirtualNode-");
		sb.append(v);
		sb.append("-");
		sb.append(p);
		sb.append("-");
		sb.append(r);
		sb.append("-");
		sb.append(System.currentTimeMillis());
		return sb.toString();
	}
	private int rank(String v) {
		return ranks.get(v);
	}
	
	private Order orders;
	private void ordering() {
		Order best = initialOrdering();
		
		int maxIterations = MAX_ORDERING_ITERATIONS;
		
		for (int i = 0; i < maxIterations; i++) {			
			System.out.println("Ordering Iteration " + i);
			Order order = copy(best);
			
			System.out.println("\tMedian sorting");
			wmedian(order, i);
			
			System.out.println("\tTransposing");
			transpose(order);
			
			System.out.println("\tCross testing");
			if (crossingCount(order) < crossingCount(best)) {
				System.out.println("\t\tImprovement made!");
				best = order;
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
			List<String> rord = order.get(r);
			final HashMap<String, Float> medians = new HashMap<String, Float>();
			
			for (String v : rord) {
				medians.put(v, medianValue(order, v, r-rd));
			}
			// sort(order[r], median)
			Collections.sort(rord, new Comparator<String>() {
				public int compare(String a, String b) {
					int s = Mathf.sign(medians.get(a) - medians.get(b));
					if (s == 0)
						return a.compareTo(b);
					return s;
				}
			});
		}
		
	}
	private float[] adjPosition(Order order, String v, int adjRank) {
		if (!order.containsKey(adjRank))
			return new float[] {};
		
		List<String> adjacent = new LinkedList<String>();
		List<Float> poses = new LinkedList<Float>();
		
		int i = 0;
		for (String rank : order.get(adjRank)) {
			if (db.hasParent(v, rank) || db.hasParent(rank, v)) {
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
//						System.out.println(crossingVW + ", " + crossingWV);
						improved = true;
					}
				}
			}			
		}
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
		Order orders = new Order();
		HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
		LinkedList<String> frontier = new LinkedList<String>();
		for (String s : virtual.getVertices()) {
			frontier.add(s);
			break;
		}
		
		while (!frontier.isEmpty()) {
			String v = frontier.pop();
			if (visited.containsKey(v))
				continue;
			visited.put(v, true);
			int rank = rank(v);
			if (!orders.containsKey(rank))
				orders.put(rank, new ArrayList<String>());
			orders.get(rank).add(v);
			
			for (String p : virtual.getOutgoingVertices(v)) {
				frontier.add(p);
			}
			for (String c : virtual.getIncomingVertices(v)) {
				frontier.add(c);
			}
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

