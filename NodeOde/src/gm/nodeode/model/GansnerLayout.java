package gm.nodeode.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

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
		
		// start with leaf nodes and work our way backwards
		for (String v : vertices()) {
			if (!db.hasParents(v)) {
				frontier.add(v);
			}
		}
		
		while (!frontier.isEmpty()) {
			String v = frontier.pop();
			if (ranks.containsKey(v))
				continue;
			
			int rank = 0;
			for (String child : db.findChildren(v)) {
				rank = Math.min(rank, ranks.get(child)-1);
			}
			ranks.put(v, rank);
			if (rank > maxrank) maxrank = rank;
			if (rank < minrank) minrank = rank;
			
			parenting: 
			for (String parent : db.findParents(v)) {
				if (ranks.containsKey(parent))
					continue;
				for (String child : db.findChildren(parent)) {
					if (!ranks.containsKey(child) && !frontier.contains(child)) {
						// not ready yet
						continue parenting;
					}
				}
				frontier.add(parent);
			}
		}
	}
	private int rank(String v) {
		return ranks.get(v);
	}
	
	private HashMap<Integer, String[]> orders;
	private void ordering() {
		orders = new HashMap<Integer, String[]>();
		// init order
		for (int i = minrank; i <= maxrank; i++) {
			
		}
	}
	
	private void position() {
		
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
