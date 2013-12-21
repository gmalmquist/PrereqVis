package gm.nodeode.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class NodeIO {

	public static List<ICourse> read(String path) {
		return read(new File(path));
	}
	
	public static List<ICourse> read(File file) {
		List<ICourse> nodesList = new LinkedList<ICourse>();
		
		HashMap<String, ICourse> nodes = new HashMap<String, ICourse>();
		List<String> empties = new LinkedList<String>();
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(
					new FileReader(file));
		} catch (Exception e) {
			return nodesList;
		}
		
		String line = null;
		while (true) {
			try {
				line = in.readLine();
			} catch (Exception e) {
				break;
			}
			if (line == null) break;
			
			line = line.trim();
			String[] parts = line.split("\t");
			
			if (parts.length != 4)
				continue;
			
			Course node = new Course(parts[0], parts[1], parts[2].split(","));
			if (!nodes.containsKey(node.getUID()))
				nodes.put(node.getUID(), node);
			
//			String[] preqs = parts[3].replaceAll("([A-Z]+) ([0-9X]{4})", "$1::$2").split(" ");
			
			String[] prereqTokens = parts[3].split(",");
			for (int i = 0; i < prereqTokens.length; i++)
				prereqTokens[i] = prereqTokens[i].trim();
			
			String[] preqIDs = getPrereqCourse(nodes, empties, prereqTokens);

			for (String preq : preqIDs)
				node.addParent(preq);
		}
		
		try {
			in.close();
		} catch (Exception e) {
			System.err.println("Warning: possible resource leak: " + e);
		}
		
		for (String empty : empties) {
			if (!nodes.containsKey(empty)) {
				nodes.put(empty, new LeafCourse(empty));
			}
		}
		
		for (String s : nodes.keySet())
			nodesList.add(nodes.get(s));
		
		return nodesList;
	}
	
	private static String[] getPrereqCourse(
			HashMap<String, ICourse> courses, List<String> extras, String[] src) {
		LinkedList<String> operands = new LinkedList<String>();
		
		LinkedList<String> addedGroups = new LinkedList<String>();
		LinkedList<String> extraCandis = new LinkedList<String>();
		
		for (int i = 0; i < src.length; i++) {
			String s = src[i];
			if (s.equals("&") || s.equals("|")) {
				// operator!
				boolean and = s.equals("&");
				
				String opA = operands.pop();
				String opB = operands.pop();

				List<String> locals = new LinkedList<String>();
				
				boolean needA = true;
				boolean needB = true;
				
				// Collapse operands, if possible
				if (courses.containsKey(opA)) {
					ICourse c = courses.get(opA);
					if (c instanceof PrereqGroup) {
						PrereqGroup p = (PrereqGroup)c;
						if (p.isAnd() == and) {
							Collections.addAll(locals, p.getChildren());
							if (addedGroups.contains(p.getUID()))
								extraCandis.add(p.getUID()); // might now be extraneous
							needA = false;
						}
					}
				}

				if (courses.containsKey(opB)) {
					ICourse c = courses.get(opB);
					if (c instanceof PrereqGroup) {
						PrereqGroup p = (PrereqGroup)c;
						if (p.isAnd() == and) {
							Collections.addAll(locals, p.getChildren());
							if (addedGroups.contains(p.getUID()))
								extraCandis.add(p.getUID()); // might now be extraneous
							needB = false;
						}
					}
				}

				if (needA) {
					if (!courses.containsKey(opA)) {
						extras.add(opA);
					}
					locals.add(opA);
				}
				
				if (needB) {
					if (!courses.containsKey(opB)) {
						extras.add(opB);
					}
					locals.add(opB);
				}
				
				// We can alphabetize the local operands because
				// these operators are all commutative. Also 
				// we're at Georgia Tech and WE CAN DO THAT.
				Collections.sort(locals);
				
				PrereqGroup group = new PrereqGroup(locals.toArray(new String[locals.size()]), and);
				String gid = group.getUID();
				
				if (!courses.containsKey(gid)) {
					courses.put(gid, group);
					addedGroups.add(gid);
				}
				
				operands.push(gid);
			} else {
				operands.push(s);
			}
		}
		
		if (operands.size() != 1) {
			throw new RuntimeException("Error parsing prerequisites (didn't end with 1 operand!)");
		}

		// Remove extraneous operands
		while (!extraCandis.isEmpty()) {
			String p = extraCandis.pop();
			if (!courses.containsKey(p))
				continue;
			
			if (count(addedGroups, p) == count(extraCandis, p)+1) {
				courses.remove(p);
				addedGroups.remove(p);
			}
		}

		String preqID = operands.pop();
		
		// check to see if we can expand the course
		// by just return its child list (only valid
		// if it's an AND of other courses)
		ICourse c = courses.get(preqID);
		if (c != null && c instanceof PrereqGroup) {
			PrereqGroup pg = (PrereqGroup)c;
			if (pg.isAnd()) {
				if (addedGroups.contains(pg.getUID()))
					courses.remove(pg);
				return pg.getChildren();
			}
		}
		
		// otherwise, just return an array of one
		return new String[] { preqID };
	}
	
	private static int count(Collection<String> ls, String s) {
		int count = 0;
		for (String l : ls)
			if (l.equals(s))
				count++;
		return count;
	}
	
	private static String splitSortJoin(String s, String split) {
		if (!s.contains(split)) return s;
		String[] arr = s.split(split);
		Arrays.sort(arr);
		StringBuffer sb = new StringBuffer(s.length());
		for (String str : arr) {
			if (sb.length() > 0) sb.append(split);
			sb.append(str);
		}
		return sb.toString();
	}
}
