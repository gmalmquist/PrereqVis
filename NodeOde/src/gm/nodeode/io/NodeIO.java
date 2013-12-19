package gm.nodeode.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class NodeIO {

	public static List<ICourse> read(String path) {
		return read(new File(path));
	}
	
	public static List<ICourse> read(File file) {
		List<ICourse> nodesList = new LinkedList<ICourse>();
		
		Hashtable<String, ICourse> nodes = new Hashtable<String, ICourse>();
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
			
			String[] preqs = parts[3].replaceAll("([A-Z]+) ([0-9X]{4})", "$1::$2").split(" ");
			for (String preq : preqs) {
				preq = preq.trim().replaceAll("::", " ");
				if (preq.length() == 0)
					continue;
				node.addParent(preq);
				if (!nodes.containsKey(preq)) {
					if (preq.contains(",")) {
						String[] ps = preq.split(",");
						nodes.put(preq, new PrereqGroup(ps));
						for (String pr : ps) {
							if (!nodes.containsKey(pr))
								empties.add(pr);
						}
					} else {
						empties.add(preq);
					}
				}
			}
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
}
