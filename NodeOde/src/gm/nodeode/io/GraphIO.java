package gm.nodeode.io;

import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Edge;
import gm.nodeode.math.graph.Graph;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeNode;
import gm.nodeode.model.Visode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class GraphIO {
	
	private static String urlFor(String name) {
		StringBuffer sb = new StringBuffer();
		if (name.matches("^[A-Z]+ \\d+$")) {
			// https://oscar.gatech.edu/pls/bprod/bwckctlg.p_disp_course_detail?cat_term_in=201408&subj_code_in=CS&crse_numb_in=4240
			String[] parts = name.split(" ");
			sb.append("https://oscar.gatech.edu/pls/bprod/bwckctlg.p_disp_course_detail?cat_term_in=201408&subj_code_in=");
			sb.append(parts[0]);
			sb.append("&crse_numb_in=");
			sb.append(parts[1]);
		}
		return sb.toString();
	}
	
	private static void jsonPair(StringBuffer sb, String name, Object value) {
		sb.append("\"");
		sb.append(name);
		sb.append("\":\"");
		sb.append(value);
		sb.append("\"");
	}
	
	public static String toJson(OdeAccess access) {
		StringBuffer sb = new StringBuffer(1024);
		sb.append("{ ");
		
		boolean first = true;
		
		sb.append("\"vertices\":[ ");
		for (Visode v : access.getVisodes()) {
			String type = "";
			if (v.getType() == Visode.TYPE_LINK) {
				type = "link";
			} else if (v.getType() == Visode.TYPE_NODE) {
				type = "node";
			} else if (v.getType() == Visode.TYPE_SPACER) {
				continue;
			}
			
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			
			sb.append("{ ");
			
			jsonPair(sb, "uid", v.getUID());
			sb.append(", ");

			jsonPair(sb, "name", v.getDisplayName());
			sb.append(", ");

			jsonPair(sb, "longname", v.getLongName());
			sb.append(", ");

			jsonPair(sb, "type", type);
			sb.append(", ");
			
			jsonPair(sb, "url", urlFor(v.getUID()));
			sb.append(", ");

			jsonPair(sb, "x", v.getCenter().x);
			sb.append(", ");
			
			jsonPair(sb, "y", v.getCenter().y);
			sb.append(", ");
			
			jsonPair(sb, "z", v.getCenter().z);
			sb.append(", ");
			
			jsonPair(sb, "r", v.getRadius());
			
			sb.append(" }");
		}
		sb.append(" ]");
		
		first = true;
		sb.append(", ");
		sb.append("\"edges\":[ ");
		for (String v : access.getOdes()) {
			for (String p : access.findParents(v)) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				
				sb.append("{ ");
				jsonPair(sb, "tail", v);
				sb.append(", ");
				jsonPair(sb, "head", p);
				sb.append(" }");
			}
		}
		sb.append(" ]");
		
		sb.append(" }");
		return sb.toString();
	}
	
	public static void saveJson(PrintStream out, OdeAccess access) {
		out.println(toJson(access));
		out.flush();
	}
	
	public static void saveJson(String name, OdeAccess access) throws IOException {
		PrintStream out = new PrintStream(new File(name));
		saveJson(out, access);
		out.close();
	}
	
	public static void saveGraph(String name, Graph graph) throws IOException {
		PrintStream out = new PrintStream(new File(name));
		saveGraph(out, graph);
		out.close();
	}
	public static void saveVisodes(String name, Iterable<Visode> visodes) throws IOException {
		PrintStream out = new PrintStream(new File(name));
		saveVisodes(out, visodes);
		out.close();
	}
	public static Graph readGraph(String name) {
		try {
			InputStream in = new FileInputStream(new File(name));
			Graph g = readGraph(in);
			in.close();
			return g;
		} catch (Exception e) {
			System.err.println(name + ": " + e);
			return null;
		}
	}
	public static Visode[] readVisodes(String name) {
		try {
			InputStream in = new FileInputStream(new File(name));
			Visode[] v = readVisodes(in);
			in.close();
			return v;
		} catch (Exception e) {
			System.err.println(name + ": " + e);
			return null;
		}
	}
	
	public static void saveGraph(PrintStream out, Graph graph) {
		out.println("graph-start");
		out.println("vertices: " + graph.vertexCount());
		for (String v : graph.getVertices()) {
			out.println("vertex: " + v);
		}
		out.println("edgelist");
		for (String v : graph.getVertices()) {
			out.println("v: " + v);
			for (Edge vu : graph.getEdges(v)) {
				out.println("u: " + vu.head);
			}
		}
		
		out.println("graph-end");
		out.flush();
	}
	
	public static Graph readGraph(InputStream sin) {
		Graph graph = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(sin));
		
		int vertexCount = -1;
		String cvertex = null;
		
		String line = null;
		while (true) {
			try {
				line = in.readLine();
			} catch (Exception e) {
				break;
			}
			if (line == null) break;
			
			line = line.trim();
			
			if (line.equals("graph-start")) {
				graph = new Graph();
				continue;
			}
			
			if (line.equals("graph-end")) {
				break;
			}
			
			if (line.startsWith("vertices: ")) {
				vertexCount = Integer.parseInt(line.substring("vertices: ".length()).trim());
				continue;
			}
			
			if (line.startsWith("vertex: ")) {
				String v = line.substring("vertex: ".length()).trim();
				graph.addVertex(v);
				continue;
			}
			
			if (line.startsWith("v: ")) {
				cvertex = line.substring("v: ".length()).trim();
				continue;
			}
			
			if (line.startsWith("u: ")) {
				String u = line.substring("u: ".length()).trim();
				graph.addEdge(cvertex, u);
				continue;
			}
		}
		
		return graph;
	}
	
	public static void saveVisodes(PrintStream out, Iterable<Visode> visodes) {
		int count = 0;
		for (Visode v : visodes) {
			count++;
		}
		
		out.println("visodes: " + count);
		for (Visode v : visodes) {
			out.println("v: " + safe(v.getUID()));
			out.println("d: " + safe(v.getDisplayName()));
			out.println("t: " + v.getType());
			out.println("x: " + v.getCenter().x);
			out.println("y: " + v.getCenter().y);
			out.println("z: " + v.getCenter().z);
			out.println("r: " + v.getRadius());
		}
		out.println("visodes-over");
		
		out.flush();
	}
	
	public static Visode[] readVisodes(InputStream sin) {
		Visode[] nodes = null;
		int index = -1;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(sin));
		
		String line = null;
		while (true) {
			try {
				line = in.readLine();
			} catch (Exception e) {
				break;
			}
			if (line == null) {
				break;
			}
			
			if (line.startsWith("visodes: ")) {
				nodes = new Visode[Integer.parseInt(line.substring("visodes: ".length()).trim())];
				continue;
			} else if (line.startsWith("visodes-over")) {
				break;
			}
			
			int colon = line.indexOf(':');
			if (colon < 0) continue;
			
			String a = line.substring(0, colon).trim();
			String v = line.substring(colon+1).trim();
			
			// v d t x y z r
			
			if (a.equals("v")) {
				if (index+1 >= nodes.length) {
					break;
				}
				nodes[++index] = new OdeNode(v);
			} else if (a.equals("d")) {
				nodes[index].setDisplayName(v);
			} else if (a.equals("t")) {
				nodes[index].setType(Integer.parseInt(v));
			} else if (a.equals("x")) {
				nodes[index].moveBy(Pt.P(Float.parseFloat(v), 0, 0));
			} else if (a.equals("y")) {
				nodes[index].moveBy(Pt.P(0, Float.parseFloat(v), 0));
			} else if (a.equals("z")) {
				nodes[index].moveBy(Pt.P(0, 0, Float.parseFloat(v)));
			} else if (a.equals("r")) {
				nodes[index].setRadius(Float.parseFloat(v));
			}
		}
		
		return nodes;
	}
	
	private static String safe(String s) {
		return s.replaceAll("\n", "[~br~]");
	}
	private static String unsafe(String s) {
		return s.replaceAll("\\[~br~\\]", "\n");
	}
}
