package gm.nodeode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;

import gm.nodeode.io.ICourse;
import gm.nodeode.io.Course;
import gm.nodeode.io.PrereqGroup;
import gm.nodeode.io.NodeIO;
import gm.nodeode.math.graph.Graph;
import gm.nodeode.model.GansnerLayout;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeGroup;
import gm.nodeode.model.OdeManager;
import gm.nodeode.model.OdeNode;
import gm.nodeode.model.Visode;
import gm.nodeode.view.NodeView;

public class NodeOde {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Node Ode");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		OdeAccess tinyManage = new OdeManager();
		
		
		NodeView view = new NodeView(tinyManage, new GansnerLayout(tinyManage));
		view.clear();

		HashMap<String, Visode> odeTable = new HashMap<String, Visode>();
		Graph mainGraph = new Graph();
		
		System.out.println("Reading in data");
		List<ICourse> nodes = NodeIO.read("D:\\Programming\\Projects\\Oscar\\data_math.txt");
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
				for (String p : node.getParents())
					mainGraph.addEdge(node.getUID(), p);
			}
		}
		
		System.out.println("Filtering data");
		
		// Choose only undergraduate CS classes
		List<String> keyClasses = new LinkedList<String>();
		String filter = classFilter("MATH", LEVEL_UNDERGRADUATE);
		for (String v : mainGraph.getVertices()) {
			if (v.matches(filter)) {
				keyClasses.add(v);
			}
		}
		mainGraph = mainGraph.getDirectedSubGraph(keyClasses, true);
		
		/*// Filter out graduate courses
		for (String v : odeTable.keySet()) {
			if (v.matches(".*?[A-Z]+ [5678]+\\d+.*")) {
				mainGraph.removeVertex(v);
			}
		}*/
		
		System.out.println("Filtering OR nodes");
		// Filter out stranded OR nodes
		for (String v : odeTable.keySet()) {
			if ((v.contains("|")||v.contains("&")) && mainGraph.getIncomingVertices(v).isEmpty()) {
				mainGraph.removeVertex(v);
			}
		}

		System.out.println("Finding largest subgraph");
		List<Graph> disjoint = mainGraph.getConnectedSubgraphs();
		Graph choice = disjoint.get(disjoint.size()-1); // choose largest connected subgraph
		
		System.out.println("Copying to view");
		// Copy to view
		for (String s : choice.getVertices()) {
			Visode ode;
			if (odeTable.containsKey(s)) {
				ode = odeTable.get(s);
			} else {
				ode = new OdeNode(s);
				System.err.println("Warning: no name entry for " + s);
			}
			tinyManage.register(ode);
		}
		
		for (String s : choice.getVertices()) {
			for (String p : choice.getOutgoingVertices(s))
				tinyManage.addParent(s, p);
		}
		
		frame.add(view);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
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
