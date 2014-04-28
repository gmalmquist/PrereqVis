package gm.nodeode;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import gm.debug.Blog;
import gm.nodeode.io.Data;
import gm.nodeode.io.GraphIO;
import gm.nodeode.io.ICourse;
import gm.nodeode.io.Course;
import gm.nodeode.io.PrereqGroup;
import gm.nodeode.io.CourseIO;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.math.graph.Graph;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeManager;
import gm.nodeode.model.OdeNode;
import gm.nodeode.model.Visode;
import gm.nodeode.view.GraphRenderer;
import gm.nodeode.view.SaveImageButton;
import gm.nodeode.view.Stitcher;

/**
 * Main class to kick stuff off
 * @author Garrett
 *
 */
public class NodeOde {

	public static final boolean FULLNAMES = false;
	
	public static void main(String[] args) throws Exception {
//		generateJsonMap(new String[] { "CS", "MATH", "PSYC" });//, "BIOL", "PHYS", "BMED", "ME", "MGT", "CX", "CHEM", "CHBE" };
		
		generateImage("CS", LEVEL_UNDERGRADUATE);
	}
	
	public static void generateJsonMap(String[] majors) throws Exception {
		String[] levelCodes = { LEVEL_UNDERGRADUATE, LEVEL_GRADUATE, LEVEL_ANY };
		String[] levelNames = { "Undergraduate", "Graduate", "Both" };
		
		StringBuffer sb = new StringBuffer(majors.length * levelCodes.length * 1024 * 10);
		for (String major : majors) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("'" + major.toUpperCase() + "':{");
			for (int i = 0; i < levelCodes.length; i++) {
				Blog.log("Graph", major + ", " + levelNames[i]);
				if (i > 0) {
					sb.append(", ");
				}
				sb.append("'" + levelNames[i] + "':");
				String json = createJSON(loadGraphs(major, levelCodes[i]));
				sb.append(json);
			}
			sb.append("}");
		}
		
		String path = "graph-data.js";
		PrintStream out = new PrintStream(path);
		out.println("var GRAPH_MAP = {");
		out.println(sb.toString());
		out.println("};");
		out.close();
		
		Blog.log("Done.");
	}
	
//	String major = "CS";
//	String level = LEVEL_UNDERGRADUATE;
//	// Uncomment these lines to change what course levels to use in the graph.
////	level = LEVEL_ANY;
////	level = LEVEL_GRADUATE;
	
	public static String createJSON(OdeAccess[] graphs) {
		StringBuffer sb = new StringBuffer(graphs.length * 512);
		sb.append("[");
		
		boolean first = true;
		
		int x0 = 0;
		int y0 = 0;
		int mw = 0;
		int mh = 0;
		for (OdeAccess graph : graphs) {
			int mX = 0;
			int mY = 0;
			for (Visode v : graph.getVisodes()) {
				Pt c = v.getCenter();
				if (c.ix() > mX) mX = c.ix();
				if (c.iy() > mY) mY = c.iy();
				v.setCenter(Pt.P(c.x + x0, c.y + y0, c.z));
			}
			
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(GraphIO.toJson(graph));
			
			if (mX > mw) {
				mw = mX;
			}
			if (mh < mY) {
				mh = mY;
			}
			
			y0 += mY;
			
			if (y0 >= mh) {
				y0 = 0;
				x0 += mw;
				mw = 0;
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
	
	public static OdeAccess[] loadGraphs(String major, String level) {
		final HashMap<String, Visode> odeTable = new HashMap<String, Visode>();
		Graph mainGraph = new Graph();
		
		final HashMap<String, String> longnames = new HashMap<String, String>();
		
		System.out.println("Reading in data");
		List<ICourse> nodes = CourseIO.read(Data.getStream("data_" + major.toLowerCase() + ".txt"));
		System.out.println("Converting to visnodes");
		for (ICourse gnode : nodes) {
			
			if (gnode instanceof PrereqGroup) {
				PrereqGroup group = (PrereqGroup)gnode;
				
				OdeNode gode = new OdeNode(gnode.getUID(), group.isAnd() ? "ALL" : "ANY");
				odeTable.put(gode.getUID(), gode);
				
				mainGraph.addVertex(gode.getUID());
				
				for (String s : group.getChildren()) {
					mainGraph.addEdge(gode.getUID(), s);
				}
			} else {
				Visode ode = new OdeNode(gnode.getUID(), gnode.getName());
				mainGraph.addVertex(ode.getUID());
				odeTable.put(ode.getUID(), ode);
			}

			if (gnode instanceof Course) {
				Course node = (Course)gnode;
				longnames.put(node.getUID(), node.getFullName());
				for (String p : node.getParents())
					mainGraph.addEdge(node.getUID(), p);
			}
		}
		
		System.out.println("Filtering data");
		
		// Choose only undergraduate CS classes
		List<String> keyClasses = new LinkedList<String>();
		String filter = classFilter(".*", level);
		for (String v : mainGraph.getVertices()) {
			if (v.matches(filter)) {
				keyClasses.add(v);
			}
		}
		mainGraph = mainGraph.getDirectedSubGraph(keyClasses, true);
		
		System.out.println("Filtering OR nodes");
		// Filter out stranded OR nodes
		for (String v : odeTable.keySet()) {
			if ((v.contains("|")||v.contains("&")) && mainGraph.getIncomingVertices(v).isEmpty()) {
				mainGraph.removeVertex(v);
			}
		}

		final List<Graph> disjoint = mainGraph.getConnectedSubgraphs();
		Collections.sort(disjoint, new Comparator<Graph>() {
			public int compare(Graph A, Graph B) {
				return B.vertexCount() - A.vertexCount();
			}
		});
		
		int nonEmpty = 0;
		for (Graph g : disjoint) {
			if (g.vertexCount() > 0) {
				nonEmpty++;
			}
		}
		
		OdeAccess[] dbs = new OdeAccess[nonEmpty];
		
		int subgraphIndex = 0;
		for (Graph choice : disjoint) {
			if (choice.vertexCount() == 0)
				continue;
			
			
			OdeAccess choiceAccess = new OdeManager();
			
			System.out.println("\nGenerating subgraph " + subgraphIndex);
				// Copy to view
			for (String s : choice.getVertices()) {
				Visode ode;
				if (odeTable.containsKey(s)) {
					ode = odeTable.get(s);
				} else {
					ode = new OdeNode(s);
					System.err.println("Warning: no name entry for " + s);
				}
				if (longnames.containsKey(ode.getUID())) {
					ode.setLongName(longnames.get(ode.getUID()));
				}
				choiceAccess.register(ode);
			}
			
			for (String s : choice.getVertices()) {
				for (String p : choice.getOutgoingVertices(s))
					choiceAccess.addParent(s, p);
			}
			
			dbs[subgraphIndex] = GraphRenderer.layout(choiceAccess);
			subgraphIndex++;
		}

		return dbs;
	}
	
	public static void generateImage(String major, String level) {

		JFrame frame = new JFrame("Node Ode");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final SaveImageButton saver = new SaveImageButton();

		final HashMap<String, Visode> odeTable = new HashMap<String, Visode>();
		Graph mainGraph = new Graph();
		
		final HashMap<String, String> longnames = new HashMap<String, String>();
		
		System.out.println("Reading in data");
		List<ICourse> nodes = CourseIO.read(Data.getStream("data_" + major.toLowerCase() + ".txt"));
		System.out.println("Converting to visnodes");
		for (ICourse gnode : nodes) {
			
			if (gnode instanceof PrereqGroup) {
				PrereqGroup group = (PrereqGroup)gnode;
				
				OdeNode gode = new OdeNode(gnode.getUID(), group.isAnd() ? "ALL" : "ANY");
				odeTable.put(gode.getUID(), gode);
				
				mainGraph.addVertex(gode.getUID());
				
				for (String s : group.getChildren()) {
					mainGraph.addEdge(gode.getUID(), s);
				}
			} else {
				Visode ode = new OdeNode(gnode.getUID(), gnode.getName());
				mainGraph.addVertex(ode.getUID());
				odeTable.put(ode.getUID(), ode);
			}

			if (gnode instanceof Course) {
				Course node = (Course)gnode;
				longnames.put(node.getUID(), node.getFullName());
				for (String p : node.getParents())
					mainGraph.addEdge(node.getUID(), p);
			}
		}
		
		System.out.println("Filtering data");
		
		// Choose only undergraduate CS classes
		List<String> keyClasses = new LinkedList<String>();
		String filter = classFilter(".*", level);
		for (String v : mainGraph.getVertices()) {
			if (v.matches(filter)) {
				keyClasses.add(v);
			}
		}
		mainGraph = mainGraph.getDirectedSubGraph(keyClasses, true);
		
		System.out.println("Filtering OR nodes");
		// Filter out stranded OR nodes
		for (String v : odeTable.keySet()) {
			if ((v.contains("|")||v.contains("&")) && mainGraph.getIncomingVertices(v).isEmpty()) {
				mainGraph.removeVertex(v);
			}
		}

		final List<Graph> disjoint = mainGraph.getConnectedSubgraphs();
		Collections.sort(disjoint, new Comparator<Graph>() {
			public int compare(Graph A, Graph B) {
				return B.vertexCount() - A.vertexCount();
			}
		});
		
		frame.add(saver);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		new Thread(new Runnable() {
			public void run() {
				List<BufferedImage> images = new LinkedList<BufferedImage>();
				
				int subgraphIndex = 0;
				for (Graph choice : disjoint) {
					if (choice.vertexCount() == 0)
						continue;
					
					
					OdeAccess choiceAccess = new OdeManager();
					
					System.out.println("\nGenerating subgraph " + subgraphIndex++);
						// Copy to view
					for (String s : choice.getVertices()) {
						Visode ode;
						if (odeTable.containsKey(s)) {
							ode = odeTable.get(s);
						} else {
							ode = new OdeNode(s);
							System.err.println("Warning: no name entry for " + s);
						}
						if (longnames.containsKey(ode.getUID())) {
							ode.setLongName(longnames.get(ode.getUID()));
						}
						choiceAccess.register(ode);
					}
					
					for (String s : choice.getVertices()) {
						for (String p : choice.getOutgoingVertices(s))
							choiceAccess.addParent(s, p);
					}
					
					BufferedImage render = GraphRenderer.layoutAndRender(choiceAccess);
					
					saver.setImage(render);
					
					images.add(render);
				}
				
				System.out.println("\n\nGenerating composite");
				BufferedImage composite = Stitcher.stitch(images.toArray(new BufferedImage[images.size()]));
				saver.setImage(composite);
				System.out.println("Done.");
			}
		}).start();
	}
	
	

	private static final String LEVEL_GRADUATE = "5-9";
	private static final String LEVEL_UNDERGRADUATE = "1-4";
	private static final String LEVEL_ANY = "1-9";
	private static String classFilter(String subj, String level) {
		StringBuffer sb = new StringBuffer(2+subj.length()+10);
		
		sb.append("^");
		sb.append(subj);
		sb.append(" [");
		sb.append(level);
		sb.append("][0-9X]{3}$");
		
		return sb.toString();
	}
	
	public static void debug(String message) {
		message = Thread.currentThread().getName() + ":\t" + message;
		System.out.println(message);
	}
}
