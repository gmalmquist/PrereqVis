package gm.nodeode.io;

import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Edge;
import gm.nodeode.math.graph.Graph;
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
