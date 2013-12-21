package gm.nodeode;

import java.util.HashMap;
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
		
		List<ICourse> nodes = NodeIO.read("D:\\Programming\\Projects\\Oscar\\data.txt");
		for (ICourse gnode : nodes) {			
			if (gnode instanceof PrereqGroup) {
				PrereqGroup group = (PrereqGroup)gnode;
				
				OdeNode gode = new OdeNode(gnode.getUID(), "ANY");
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
		
		// Filter out graduate courses
		for (String v : odeTable.keySet()) {
			if (v.matches(".*?CS [5678]+\\d+.*")) {
				mainGraph.removeVertex(v);
			}
		}
		// Filter out stranded OR nodes
		for (String v : odeTable.keySet()) {
			if (v.contains(",") && mainGraph.getIncomingVertices(v).isEmpty()) {
				mainGraph.removeVertex(v);
			}
		}

		List<Graph> disjoint = mainGraph.getConnectedSubgraphs();
		Graph choice = disjoint.get(disjoint.size()-1); // choose largest connected subgraph
		
		// Copy to view
		for (String s : choice.getVertices()) {
			tinyManage.register(odeTable.get(s));
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
	
	public static void debug(String message) {
		message = Thread.currentThread().getName() + ":\t" + message;
		System.out.println(message);
	}
}
