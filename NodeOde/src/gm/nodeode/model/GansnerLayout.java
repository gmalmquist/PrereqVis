package gm.nodeode.model;

import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	public GansnerLayout(OdeAccess db) {
		super(db);
	}

	@Override
	public void doLayout() {
		rank();
		ordering();
		position();
		makeSplines();
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
	
	private HashMap<String, Integer> ranks;
	
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
			
			int depth = 0;
			
			for (String p : db.findParents(f)) {
				if (ranks.containsKey(p)) {
					depth = Math.max(depth, ranks.get(p)+1);
				}
			}
			
			if (depth > maxd)
				maxd = depth;

			ranks.put(f, depth);
			for (String s : db.findChildren(f)) {
				frontier.add(s);
			}
		}
		
		for (String v : vertices()) {
			ranks.put(v, maxd - ranks.get(v));
		}
		
		maxrank = maxd;
		minrank = 0;
	}
	private int rank(String v) {
		return ranks.get(v);
	}
	
	private HashMap<Integer, List<String>> orders;
	private void ordering() {
		orders = new HashMap<Integer, List<String>>();
		// init order
		HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
		LinkedList<String> frontier = new LinkedList<String>();
		for (String s : vertices()) {
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
			
			for (String p : db.findParents(v)) {
				frontier.add(p);
			}
			for (String c : db.findChildren(v)) {
				frontier.add(c);
			}
		}
	}
	
	private void position() {
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
				db.find(v).setCenter(Pt.P(
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
		return db.find(s).radius();
	}
}
