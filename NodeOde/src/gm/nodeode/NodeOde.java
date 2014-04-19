package gm.nodeode;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import gm.nodeode.io.Data;
import gm.nodeode.io.ICourse;
import gm.nodeode.io.Course;
import gm.nodeode.io.PrereqGroup;
import gm.nodeode.io.CourseIO;
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
	
	public static void main(String[] args) {
		String major = "CS";
		String level = LEVEL_UNDERGRADUATE;
		// Uncomment these lines to change what course levels to use in the graph.
//		level = LEVEL_ANY;
//		level = LEVEL_GRADUATE;
		
		
		JFrame frame = new JFrame("Node Ode");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		final SaveImageButton saver = new SaveImageButton();

		final HashMap<String, Visode> odeTable = new HashMap<String, Visode>();
		Graph mainGraph = new Graph();
		
		
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
