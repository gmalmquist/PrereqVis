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
import java.util.Iterator;
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
	private static final int MAX_RANK_RETRYING = 100;
	private static final boolean LOWER_LEAVES = true;
	private static final boolean SHIFT_AVERAGE = false;
	
	private Graph virtual;
	private HashMap<String, Integer> ranks;
	private Order orders;
	private HashMap<String, String> virtIDs = new HashMap<String, String>();
	private HashMap<String, Pt> positions;

	private int step = 0;
	private int maxrank = Integer.MIN_VALUE, minrank = Integer.MAX_VALUE;

	public GansnerLayout(OdeAccess db) {
		super(db);
	}

	private Graph original;
	@Override
	public void doLayout() {
		if (ranks != null) {
			virtIDs.clear();
			orders = null;
			positions = null;
			virtual = null;
			ranks = null;
		}
		
		System.out.println("Copying original");
		if (original == null) {
			original = db.copyGraph();
		} else {
			graphToDB(original);
		}
		
		// Operation order identical to paper
		System.out.println("RANKING");
		rank();
		System.out.println("ORDERING");
		ordering();
		System.out.println("POSITIONING");
		position();
		makeSplines();
		
		// push results
		System.out.println("Pushing results to view...");
		virtualToDB();
		
		System.out.println("Modified Gansner Layout complete.");
	}

	private void virtualToDB() {
		graphToDB(this.virtual);
	}
	private void graphToDB(Graph graph) {
		HashMap<String, String> names = new HashMap<String, String>();
		for (String v : graph.getVertices()) {
			Visode o = db.find(v);
			String name = o == null ? v : String.valueOf(((OdeNode)o).getDisplayName());
			names.put(v, name);
		}
		
		db.clear();
		for (String v : graph.getVertices()) {
			Visode ode = new OdeNode(v, names.get(v));
			if (ode.getUID().matches("\\d+")) {
				if (graph.getEdges(v).size() == 0) {
					// spacer!
					ode.setType(Visode.TYPE_SPACER);
				} else {
					ode.setType(Visode.TYPE_LINK);
				}
			}
			if (positions != null && positions.containsKey(v)) {
				ode.setCenter(positions.get(v));
			}
			db.register(ode);
			for (String o : graph.getOutgoingVertices(v))
				db.addParent(v, o);
		}
	}
	
	@Override
	public void doLayoutStep() {
		switch (step++) {
		case 0: rank(); break;
		case 1: ordering(); break;
		case 2: position(); break;
		case 3: makeSplines(); break;
		}
	}
	
	
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
		
		HashMap<String, Integer> invalidTicker = new HashMap<String, Integer>();
		int invalidCap = MAX_RANK_RETRYING;

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
			
			if (!rankValid) {
				if (!invalidTicker.containsKey(f))
					invalidTicker.put(f, 1);
				else
					invalidTicker.put(f, invalidTicker.get(f)+1);
				int count = invalidTicker.get(f);
				if (count > invalidCap) {
					// NO MORE
					System.err.println("Max Rank Invalidity Hit: " + f);
					ranks.put(f, depth+1);
				} else {
					frontier.add(f);
				}
			}
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
			
			if (LOWER_LEAVES) {
				if (!db.hasChildren(v))
					r++;
				if (v.contains("&") || v.contains("|"))
					r++;
			}

			if (!rankings.containsKey(r))
				rankings.put(r, new LinkedList<String>());
			
			rankings.get(r).add(v);
		}
		
		int nmax = 0;
		int ri = 0;
		for (int i = 0; i <= maxrank*2+1; i++) {
			if (!rankings.containsKey(i) || rankings.get(i).isEmpty())
				continue;
			for (String v : rankings.get(i)) {
				ranks.put(v, ri);
			}
			nmax = ri++;
		}
		maxrank = nmax;
		System.out.println("MinRank: " + minrank);
		System.out.println("MaxRank: " + maxrank);
		
		
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
	
	private void ordering() {
		Order best = initialOrdering();

		for (int i = minrank; i <= maxrank; i++) {
			System.out.println("\tRank " + i + " exists? " + (best.get(i) != null));
		}
		
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
			if (rord == null) {
				// what?
				System.err.println("Warning: Missing rank (" + r + ")");
				continue;
			}
			final HashMap<String, Float> medians = new HashMap<String, Float>();
			final HashMap<Integer, String> statics = new HashMap<Integer, String>();
			
			int vi = 0;
			for (String v : rord) {
				float med = medianValue(order, v, r-rd);
				if (Float.isNaN(med) || Float.isInfinite(med)) {
					System.err.println("Warning: garbage data in median sorter");
					med = 0;
				}
				// -1 indicates they should keep their original position
				if (med == -1) {
					statics.put(vi, v);
				}
				medians.put(v, med);
				vi++;
			}
			
			for (int i : statics.keySet())
				rord.remove(statics.get(i));
			
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
			
			String[] ns = rord.toArray(new String[rord.size()]);
			rord.clear();
			
			int ri = 0;
			for (int i = 0; i < ns.length + statics.size(); i++) {
				if (statics.containsKey(i)) {
					rord.add(statics.get(i));
				} else {
					rord.add(ns[ri++]);
				}
			}
			
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
		else if (P.length == 2)
			return (P[0] + P[1])/2;
		
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
				int s = edgeCount.get(b) - edgeCount.get(a);
				if (s == 0)
					return a.compareTo(b);
				return s;
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

	private boolean setsEqual(Collection<String> A, Collection<String> B) {
		if (A.size() != B.size())
			return false;
		
		List<String> C = new LinkedList<String>();
		C.addAll(A);
		C.removeAll(B);
		return C.size() == 0;
	}
	
	private void position() {
		positions = new HashMap<String, Pt>();
		
		spacedPositioning();
		
		for (int i = 0; i < 100; i++) {
			shiftBlocks(i);
		}
	}
	private void shiftBlocks(int iter) {
		HashMap<Integer, List<VertexBlock>> blocks = getVertexBlocks();
		
		boolean bottomUp = iter%2 == 0;
		
		int rank0 = bottomUp ? maxrank : minrank;
		int rank1 = bottomUp ? minrank : maxrank;
		int rankD = Mathf.sign(rank1 - rank0);
		
		for (int i = rank0; i*rankD <= rank1*rankD; i += rankD) {
			// vertices in order
			List<String> order = orders.get(i);
			// blocks sorted by number of vertices in them
			List<VertexBlock> row = blocks.get(i);
			
			for (VertexBlock block : row) {
				float minpos = -Float.MAX_VALUE;
				float maxpos = +Float.MAX_VALUE;
				
				int blockA = block.index;
				int blockB = block.index + block.length - 1;
				
				float length = (positions.get(order.get(blockB)).x + 3f*radius(order.get(blockB)))
							 - (positions.get(order.get(blockA)).x - 3f*radius(order.get(blockA)));
				
				if (block.index > 0) {
					String left = order.get(block.index-1);
					minpos = positions.get(left).x + 3f*radius(left) + length/2;
				}
				if (block.index+block.length < order.size()) {
					String right = order.get(block.index+block.length);
					maxpos = positions.get(right).x - 3f*radius(right) - length/2;
				}
				
				if (maxpos <= minpos)
					continue; // movement impossible

				float outx = block.medianOutgoing().x;
				float innx = block.medianIncoming().x;
				
				float target = bottomUp ? block.medianOutgoing().x : block.medianIncoming().x;
				if (SHIFT_AVERAGE) {
					target = (outx * block.outCount + innx * block.innCount) / (block.outCount + block.innCount);
				}

				if (target > maxpos) target = maxpos;
				if (target < minpos) target = minpos;
				
				block.moveTo(block.position().x(target));
			}
		}
	}
	private static int spiralIndex(int i, int length) {
		i %= length;
		if (i%2 == 0) {
			return length/2 + i/2;
		} else {
			return length/2 - i/2 - 1;
		}
	}
	private void position(String v, Pt p) {
		positions.put(v, p);
	}
	private HashMap<Integer, List<VertexBlock>> getVertexBlocks() {
		HashMap<Integer, List<VertexBlock>> blocks = new HashMap<Integer, List<VertexBlock>>();
		for (int i = minrank; i <= maxrank; i++) {
			List<VertexBlock> row = getVertexBlocks(i);
			Collections.sort(row, new Comparator<VertexBlock>() {
				public int compare(VertexBlock A, VertexBlock B) {
					return B.length - A.length;
				}
			});
			blocks.put(i, row);
		}
		return blocks;
	}
	private List<VertexBlock> getVertexBlocks(int rank) {
		List<String> rankList = orders.get(rank);
		List<VertexBlock> blocks = new LinkedList<VertexBlock>();
		
		String last = null;
		int i = 0;
		int blockstart = 0;
		for (String v : rankList) {
			if (last != null) {
				// adjacent vertices are part of the same block if their destinations are identical
				Collection<String> out0 = virtual.getOutgoingVertices(last);
				Collection<String> out1 = virtual.getOutgoingVertices(v);
				if (out0.size() == 0 && out1.size() == 0) {
					out0 = virtual.getIncomingVertices(last);
					out1 = virtual.getIncomingVertices(v);
				}
				if (!setsEqual(out0, out1)) {
					blocks.add(new VertexBlock(rank, blockstart, i - blockstart)); // block separator
					blockstart = i;
				}
			}
			last = v;
			i++;
		}
		if (blockstart < rankList.size())
			blocks.add(new VertexBlock(rank, blockstart, rankList.size() - blockstart));
		
		return blocks;
	}
	private void spacedPositioning() {
		List<String> spacers = new LinkedList<String>();
		
		// Insert spacers
		int maxcols = 0;
		for (int r : orders.keySet()) {
			maxcols = Math.max(maxcols, orders.get(r).size());
		}
		maxcols *= 1.5;
		
		for (int r : orders.keySet()) {
			List<String> rank = orders.get(r);
			int spacerCount = maxcols - rank.size();
			List<Integer> blocks = new LinkedList<Integer>();
			
			blocks.add(0);
			
			String last = null;
			int i = 0;
			for (String v : rank) {
				if (last != null) {
					// adjacent vertices are part of the same block if their destinations are identical
					Collection<String> out0 = virtual.getOutgoingVertices(last);
					Collection<String> out1 = virtual.getOutgoingVertices(v);
					if (out0.size() == 0 && out1.size() == 0) {
						out0 = virtual.getIncomingVertices(last);
						out1 = virtual.getIncomingVertices(v);
					}
					if (!setsEqual(out0, out1)) {
						blocks.add(i); // block separator
					}
				}
				last = v;
				i++;
			}
			
			blocks.add(rank.size());
			
			int b = 0;
			for (int s = 0; s < spacerCount; s++) {
				int bi = spiralIndex(b++, blocks.size());
//				bi = (int) (Mathf.random() * blocks.size());
				int insert = blocks.get(bi);
				
				// Have to shift indices over 'cause we inserted something
				for (int j = bi+1; j < blocks.size(); j++)
					blocks.set(j, blocks.get(j)+1);
				
				String spacer = virtual.addUniqueVertex();
				spacers.add(spacer);
				rank.add(insert, spacer);
			}
		}
		
		gridPositioning();
		
		for (String spacer : spacers) {
			virtual.removeVertex(spacer);
			positions.remove(spacer);
			for (Integer r : orders.keySet())
				orders.get(r).remove(spacer);
		}
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
			return 12;
		return db.find(s).radius();
	}

	// Because Java doesn't have typedefs
	private class Order extends HashMap<Integer, List<String>> { }
	
	private class VertexBlock implements Iterable<String> {
		public final int rank;
		public final int index;
		public final int length;
		
		public final int innCount;
		public final int outCount;
		
		private List<String> vertices;
		
		public VertexBlock(int rank, int index, int length) {
			this.rank = rank;
			this.index = index;
			this.length = length;
			
			vertices = new LinkedList<String>();
			
			List<String> rs = orders.get(rank);
			for (int i = index; i < index+length; i++) {
				vertices.add(rs.get(i));
			}
			
			HashMap<String, Boolean> it = new HashMap<String, Boolean>();
			HashMap<String, Boolean> ot = new HashMap<String, Boolean>();
			for (String v : vertices) {
				for (String o : virtual.getOutgoingVertices(v))
					ot.put(o,true);
				for (String i : virtual.getIncomingVertices(v))
					it.put(i,true);
			}
			
			this.innCount = it.size();
			this.outCount = ot.size();
		}
		
		public Pt position() {
			return Pt.P(0,0)
					.add(0.5f, positions.get(vertices.get(0)))
					.add(0.5f, positions.get(vertices.get(vertices.size()-1)));
		}
		
		public void moveBy(float x, float y) {
			for (String s : vertices)
				positions.get(s).add(x, y);
		}
		
		public void moveTo(float x, float y) {
			Pt pos = position();
			moveBy(x - pos.x, y - pos.y);
		}
		
		public void moveBy(Pt p) {
			moveBy(p.x, p.y);
		}
		
		public void moveTo(Pt p) {
			moveTo(p.x, p.y);
		}
		
		public Pt medianOutgoing() {
			Pt med = Pt.P(0,0);
			int N = 0;
			
			for (String v : vertices) {
				for (String s : virtual.getOutgoingVertices(v)) {
					med.add(positions.get(s));
					N++;
				}
			}
			if (N > 0)
				med.mul(1f/N);
			else
				return position();
						
			return med;
		}
		
		public Pt medianIncoming() {
			Pt med = Pt.P(0,0);
			int N = 0;
			
			for (String v : vertices) {
				for (String s : virtual.getIncomingVertices(v)) {
					med.add(positions.get(s));
					N++;
				}
			}
			if (N > 0)
				med.mul(1f/N);
			else
				return position();
			
			return med;
		}
		
		public Iterator<String> iterator() {
			return vertices.iterator();
		}
	}
}

