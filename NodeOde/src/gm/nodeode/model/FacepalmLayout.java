package gm.nodeode.model;

import gm.nodeode.math.CircleAdapter;
import gm.nodeode.math.Mathf;
import gm.nodeode.math.Physicist;
import gm.nodeode.math.Pt;
import gm.nodeode.math.TimeKeeper;
import gm.nodeode.view.NodeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class FacepalmLayout extends OdeLayout {

	public FacepalmLayout(OdeManager db) {
		super(db);
	}

	@Override
	public void doLayout() {
		while (!nodeStep(step+=100));
	}

	@Override
	public void doLayoutStep() {
		if (depths == null) {
			sortStepInit();
		}
	
		nodeStep(++step);
	}
	

	private boolean physics = false;
	private HashMap<String, Integer> depths;
	private HashMap<String, List<String>> childMap;
	private List<List<String>> layers;
	private int step = -1;
	private int maxDepth = 0;
	private Physicist<String> feynman;
	private CircleAdapter<String> dapt = new CircleAdapter<String>() {

		@Override
		public Pt position(String t) {
			return find(t).getCenter();
		}

		@Override
		public float radius(String t) {
			return ((OdeNode)find(t)).r;
		}

		@Override
		public float mass(String t) {
			return FacepalmLayout.this.mass(t);
		}

		@Override
		public void moveBy(String t, Pt v) {
			find(t).moveBy(v);
		}

		@Override
		public void moveTo(String t, Pt p) {
			find(t).moveTo(p);
		}
		
	};
	
	private Visode find(String s) {
		return db.find(s);
	}
	private Iterable<String> getParents(String s) {
		return db.getParents(s);
	}
	private boolean hasParents(String s) {
		return db.hasParents(s);
	}

	private HashMap<String, List<String>> calculateChildMap() {
		return calculateChildMap(true);
	}
	
	private HashMap<String, List<String>> calculateChildMap(boolean compressGroups) {
		// Maps parents to children list
		HashMap<String, List<String>> children = new HashMap<String, List<String>>();
		for (String child : db.getOdes()) {
			for (String parentz : getParents(child)) {
				String[] pars = {parentz};
				if (compressGroups) {
					Visode o = find(parentz);
					if (o instanceof OdeGroup) {
						pars = ((OdeGroup)o).getChildren();
					}
				}
				for (String parent : pars) {
					if (!children.containsKey(parent))
						children.put(parent, new LinkedList<String>());
					children.get(parent).add(child);
				}
			}
		}
		return children;
	}
	
	private HashMap<String, Integer> calculateNodeDepths() {
		// Maps parents to children list
		HashMap<String, List<String>> children = calculateChildMap();
		
		HashMap<String, Integer> depths = new HashMap<String, Integer>();
		LinkedList<String> frontier = new LinkedList<String>();
		frontier.addAll(db.getOdes());
		
		// Initialize with leaf nodes
		for (String s : db.getOdes()) {
			if (!children.containsKey(s)) {
				depths.put(s, 0);
				frontier.remove(s);
			}
		}

		LinkedList<String> completed = new LinkedList<String>();
		LinkedList<String> done = new LinkedList<String>();
		while (frontier.size() > 0) {
			nope:
			for (String s : frontier) {
				int maxd = 0;
				for (String c : children.get(s)) {
					if (!depths.containsKey(c)) continue nope;
						maxd = Math.max(maxd, depths.get(c));

				}
				depths.put(s,  maxd+1);
				done.add(s);
			}
			completed.addAll(done);
		
			frontier.removeAll(done);
			done.clear();
		}
		
		for (String s : db.getOdes()) {
			Visode o = find(s);
			if (!(o instanceof OdeGroup))
				continue;
			
			OdeGroup g = (OdeGroup)o;
			
			int max = 0;
			if (depths.containsKey(s))
				max = depths.get(s);
			
			for (String k : g.getChildren()) {
				if (depths.containsKey(k))
					max = Math.max(depths.get(k), max);
			}
			
			depths.put(s, max);
			for (String k : g.getChildren()) {
				depths.put(k, max);
			}
		}
		
		return depths;
	}

	private HashMap<String, Integer> calculateNodeDepthsAlt() {
		HashMap<String, Integer> depths = new HashMap<String, Integer>();
		
		List<OdeNode> nodes = new LinkedList<OdeNode>();
		LinkedList<String> frontier = new LinkedList<String>();
		for (String s : db.getOdes()) {
			Visode o = find(s);
			if (o instanceof OdeNode) {
				nodes.add((OdeNode)o);
				if (!hasParents(s))
					frontier.add(s);
			}
		}

		int maxd = 0;
		
		while (!frontier.isEmpty()) {
			String f = frontier.pop();
			if (depths.containsKey(f))
				continue;
			
			int depth = 0;
			
			for (String p : getParents(f)) {
				if (depths.containsKey(p)) {
					depth = Math.max(depth, depths.get(p)+1);
				}
			}
			
			if (depth > maxd)
				maxd = depth;

			depths.put(f, depth);
			if (childMap.containsKey(f)) {
				for (String s : childMap.get(f)) {
					frontier.add(s);
				}
			}
		}
		
		for (OdeNode n : nodes) {
			depths.put(n.getUID(), maxd - depths.get(n.getUID()));
		}
		
		return depths;
	}
	
	private int totalChildCount(HashMap<String, List<String>> map, String node) {
		if (!map.containsKey(node))
			return 0;
		
		List<String> results = new LinkedList<String>();
		LinkedList<String> frontier = new LinkedList<String>();
		frontier.addAll(map.get(node));
		
		while (!frontier.isEmpty()) {
			String s = frontier.pop();
			if (results.contains(s))
				continue;
			
			results.add(s);
			if (map.containsKey(s))
				frontier.addAll(map.get(s));
		}
		
		return results.size();
	}
	
	
	
	private void sortStepInit() {
		childMap = calculateChildMap();
		depths = calculateNodeDepthsAlt();
		
		for (String s : depths.keySet()) {
			int depth = depths.get(s);
			maxDepth = Math.max(maxDepth, depth);
			
			Visode o = find(s);
			if (o instanceof OdeNode) {
				OdeNode n = ((OdeNode)o);
				if (childMap.containsKey(s)) {
//					n.r += 2*Math.log(totalChildCount(childMap, s))/Math.log(1.2);
				}
			}
		}
		
		layers = new LinkedList<List<String>>();
		for (int i = maxDepth; i >= 0; i--) {
			List<String> list = new LinkedList<String>();
			
			for (String s : depths.keySet()) {
				if (depths.get(s) == i)
					list.add(s);
			}
			
			if (!list.isEmpty())
				layers.add(list);
		}
		
		System.out.println("Max depth: " + maxDepth);
	}
	
	private float mass(String s) {
		Visode o = find(s);
		if (o instanceof OdeNode) {
			OdeNode n = (OdeNode)o;
			
			return Mathf.PI * n.r * n.r / 100f; // use area
		}
		
		if (o instanceof OdeGroup) {
			float mass = 0;
			for (String k : ((OdeGroup)o).getChildren())
				mass += mass(k);
			return mass;
		}
		
		return 1;
	}
	
	private TimeKeeper keeper = new TimeKeeper();
	private float timePassed = 0;
	private synchronized void applyPhysics() {
		if (!physics) return;
		
		timePassed = keeper.timePassed();
		
		if (feynman == null) {
			feynman = new Physicist<String>(dapt, db.getOdes());
		}
		
		
		for (String a : db.getOdes()) {
			Visode oa = find(a);
			for (String b : db.getOdes()) {
				if (a.equals(b)) continue;
				Visode ob = find(b);
				
				Pt A = oa.getCenter();
				Pt B = ob.getCenter();
				
				Pt AB = B.d().sub(A);
				
				if (AB.mag2() < 1)
					continue; // NOPE
				
				float force = 0;
				
				if (isParent(a, b) || isParent(b, a)) {
					// Links work like springs
					float C0 = isParent(a, b) ? 
							childrenLength(b) : 
							childrenLength(a);
					float C1 = isParent(a, b) ? 
							nodesLength(childMap.get(b)) : 
							nodesLength(childMap.get(a));
					
					float margin = dapt.radius(a) + dapt.radius(b);
					float strut0 = 0.5f * (C0 / Mathf.TAU);
					float strut1 = 0.5f * (C1 / Mathf.TAU);

					float ks = 10;
					float x0 = strut0 + margin;
					float x1 = strut1 + margin;
					
					float m = AB.mag();
					
					if (m > strut0)
						force = (m - x0) * ks;
					else if (m < strut1)
						force = (m - x1) * ks;
					else 
						force = 0;
				} else {
					// All other nodes repel with a quadratic decay
					
					float G = -1000;
					force = G * mass(a) * mass(b) / AB.mag2();
				}
				
				AB = AB.normalize();
				
				feynman.applyForce(a, AB.mul(force), timePassed);
			}
			
			// Things should be /above/ their prerequisites!
			for (String p : getParents(a)) {
				Visode ob = find(p);
				Pt A = oa.getCenter();
				Pt B = ob.getCenter();
				
				// vector from child to parent, should point down (+y)
				Pt AB = B.d().sub(A);
				float margin = dapt.radius(p);
				float aboveness = AB.dot(0, 1) - dapt.radius(a) - dapt.radius(p) - margin;
				
				if (aboveness <= 1) aboveness = 1;
				float force = 10f * mass(a) * mass(p) / (aboveness * aboveness);
				
				Pt dir = Pt.P(0,-1);
				
				feynman.applyForce(a, dir.d().mul(+force), timePassed);
				feynman.applyForce(p, dir.d().mul(-force), timePassed);
			}
		}
		
		feynman.update(timePassed*10);
	}
	
	private float nodesLength(Iterable<String> odes) {
		float length = 0;
		for (String s : odes) {
			Visode o = find(s);
			if (o instanceof OdeNode) {
				length += ((OdeNode)o).r * 2;
			}
		}
		return length;
	}
	
	private float childrenLength(String parent) {
		Hashtable<String, String> kids = new Hashtable<String, String>();
		LinkedList<String> frontier = new LinkedList<String>();
		frontier.add(parent);
		
		float length = 0;
		
		while (!frontier.isEmpty()) {
			String ode = frontier.pop();
			if (kids.containsKey(ode))
				continue;
			kids.put(ode, "done");
			
			if (!childMap.containsKey(ode))
				continue;
			
			for (String kid : childMap.get(ode)) {
				if (kids.containsKey(kid))
					continue; // already covered
				frontier.add(kid);
				
				// update length
				Visode o = find(kid);
				if (o instanceof OdeNode)
					length += ((OdeNode)o).r*2;
			}
		}
		
		return length;
	}
	
	
	private List<List<String>> findConnectedGroups() {
		UnionFind<String> uf = new UnionFind<String>();
		for (String s : db.getOdes()) {
			for (String p : getParents(s)) {
				uf.union(s, p);
			}
		}
		return uf.discreteGroups(db.getOdes());
	}

	private boolean isParent(String a, String b) {
		for (String p : getParents(a))
			if (p.equals(b))
				return true;
		return false;
	}
	private boolean isChild(String a, String b) {
		if (!childMap.containsKey(a))
			return false;
		for (String p : childMap.get(a))
			if (p.equals(b))
				return true;
		return false;
	}
	

	private boolean containsAny(List<String> A, Iterable<String> B) {
		for (String b : B) {
			if (A.contains(b))
				return true;
		}
		return false;
	}
	
	private boolean containsAll(List<String> A, Iterable<String> B) {
		for (String b : B)
			if (!A.contains(b))
				return false;
		return true;
	}
	
	private void enablePhysics() {
		physics = true;
	}
	
	private boolean nodeStep(int step) {
		List<String> reals = new LinkedList<String>();
		for (String s : db.getOdes()) {
			if (find(s) instanceof OdeNode)
				reals.add(s);
		}
		
		HashMap<String, OdeGroup> groups = new HashMap<String, OdeGroup>();
		for (String s : db.getOdes()) {
			Visode o = find(s);
			if (o instanceof OdeGroup) {
				OdeGroup g = (OdeGroup)o;
				for (String k : g.getChildren())
					groups.put(k, g);
			}
		}
		
		List<String[]> strata = new ArrayList<String[]>(10);
		
		LinkedList<String> used = new LinkedList<String>();
		LinkedList<String> roots = new LinkedList<String>();
		
		final List<List<String>> disjointGraphs = findConnectedGroups();
		final HashMap<String, Integer> disjointIndex = new HashMap<String, Integer>();
		int li = 0;
		for (List<String> ls : disjointGraphs) {
			for (String s : ls)
				disjointIndex.put(s, li);
			li++;
		}
		
		List<List<String>> strataL = new LinkedList<List<String>>();
		// Stratifying
		float y = 0;
		int level = 0;
		for (; level < step; level++) {
			roots.clear();			
			R: for (String s : reals) {
				if (used.contains(s)) continue;
				
				String[] fors = {s};
				if (groups.containsKey(s))
					fors = groups.get(s).getChildren();
				
				// If we're in a group, only valid if all of our siblings are valid.
				for (String ss : fors) {
					for (String p : getParents(ss)) {
						if (find(p) instanceof OdeGroup) {
							List<String> ls = new LinkedList<String>();
							for (String l : ((OdeGroup)find(p)).getChildren()) ls.add(l);
							if (containsAny(used, ls))
								continue;
							continue R;
						}
						if (!used.contains(p))
							continue R;
					}
				}
				roots.add(s);
			}
			if (roots.size() == 0) {
				level--;
				break;
			}
			
			Collections.sort(roots, new Comparator<String>() {
				public int compare(String a, String b) {
					int da = disjointIndex.get(a);
					int db = disjointIndex.get(b);
					if (da == db)
						return a.compareTo(b);
					return da - db;
				}
			});
			
			float length = 0;
			for (String r : roots)
				length += ((OdeNode)find(r)).r*2+3;
			
			float maxr = 0;
			
			int i = 0;
			for (String r : roots) {
				OdeNode n = (OdeNode) find(r);
				n.x = 1.0f * (-length/2 + length * (i+1) / (roots.size()+1));
				n.y = y - n.r;
				if (n.r > maxr) maxr = n.r;
				i++;
			}
			
			y -= maxr*5 + 5;
			
			used.addAll(roots);
			strataL.add((LinkedList<String>) roots.clone());
		}
		
		for (List<String> str : strataL)
			strata.add(str.toArray(new String[str.size()]));
		
		// Normalize Y
		int yi = 0;
		float minx = 0, maxx = 0;
		float height = 1080;
		for (String[] stratum : strata) {
			yi++;
			for (String s : stratum) {
				Visode o = find(s);
				Pt c = o.getCenter();
				c.y = height/2 - height * yi / (strata.size()+1);
				o.setCenter(c);
				if (c.x < minx) minx = c.x;
				if (c.x > maxx) maxx = c.x;
			}
		}
		
//		// Normalize X
//		if (false)
//		for (String[] stratum : strata) {
//			int i = 0;
//			for (String s : stratum) {
//				Ode o = find(s);
//				Pt c = o.getCenter();
//				c.x = minx + (maxx-minx) * i++ / (stratum.length-1);
//				o.setCenter(c);
//			}
//		}
		
		if (level > step) return false;
		
		// X-sorting
		int sorti = 0;
		int sortAmount = step - level;
		boolean up = true;
		boolean justFlipped = false;
		while (sorti <= sortAmount && strata.size() > 0) {
			boolean anySwapped = false;
			for (String[] layer : strata) {
				sorti++;
				if (sorti > sortAmount) return false;
				
				Pt[] poses = new Pt[layer.length];
				int pi = 0;
				for (String s : layer) 
					poses[pi++] = find(s).getCenter();
				
				// bubble sort is dumb but easy to animate
				boolean swaps = false;
				bubble_sort: do {
					swaps = false;
					for (int i = 0; i < layer.length-1; i++) {
						OdeNode a = (OdeNode)find(layer[i]);
						OdeNode b = (OdeNode)find(layer[i+1]);
						
						Pt pA = a.getCenter();
						Pt pB = b.getCenter();
						
						Pt fa0 = nodeForce(a.getUID(), up);
						Pt fb0 = nodeForce(b.getUID(), up);
						
						a.setCenter(pB); b.setCenter(pA);
						Pt fa1 = nodeForce(a.getUID(), up);
						Pt fb1 = nodeForce(b.getUID(), up);
						a.setCenter(pA); b.setCenter(pB);
						
						if (fa0.mag() + fb0.mag() > fa1.mag() + fb1.mag()) {
							// swap
							layer[i+1] = a.getUID();
							layer[i+0] = b.getUID();
							a.setCenter(pB);
							b.setCenter(pA);
							swaps = true;
							anySwapped = true;
						}
					}
					
					if (sorti > sortAmount)
						break bubble_sort;
				} while (swaps);
			}
			if (!anySwapped) {
				// Time to change processing direction
				if (justFlipped) {
					// Stable configuration found!
					System.out.println("STABLE");
					enablePhysics();
					return true;
				} else {
					up = !up;
					justFlipped = true;
					System.out.println("FLIP");
				}
			} else {
				justFlipped = false;
			}
		}
		return false;
	}
	
	private Pt nodeForce(String s, boolean up) {
		Pt cf = childForce(s);
		Pt pf = parentForce(s);
		
		cf = Pt.P(cf.mag(), 0);
		pf = Pt.P(pf.mag(), 0);
		
		return Pt.P(0,0)
				.add(1, cf)
				.add(1, pf);
//		return up ? childForce(s) : parentForce(s);
	}
	
	private Pt parentForce(String s) { return parentForce(s, null); }
	private Pt childForce(String s) { return childForce(s, null); }
	
	private Pt parentForce(String s, Pt pos) {
		if (pos == null) pos = find(s).getCenter();
		Pt force = Pt.P(0,0);
		
		for (String p : getParents(s)) {
			force.add(find(p).getCenter().sub(pos));
		}
		
		return force;
	}
	
	private Pt childForce(String s, Pt pos) {
		if (pos == null) pos = find(s).getCenter();
		Pt force = Pt.P(0,0);
		
		if (!childMap.containsKey(s))
			return force;
		for (String c : childMap.get(s)) {
			force.add(find(c).getCenter().sub(pos));
		}
		
		return force;
	}
	
}
