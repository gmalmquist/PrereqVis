package gm.nodeode;

import java.util.List;

import javax.swing.JFrame;

import gm.nodeode.io.ICourse;
import gm.nodeode.io.Course;
import gm.nodeode.io.PrereqGroup;
import gm.nodeode.io.NodeIO;
import gm.nodeode.model.OdeAccess;
import gm.nodeode.model.OdeGroup;
import gm.nodeode.model.OdeManager;
import gm.nodeode.model.OdeNode;
import gm.nodeode.view.NodeView;

public class NodeOde {

	public static void main(String[] args) {
		JFrame frame = new JFrame("Node Ode");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		OdeAccess mainManage = new OdeManager();
		OdeAccess tinyManage = new OdeManager();
		
		NodeView view = new NodeView(tinyManage);
		
		view.clear();
		
		List<ICourse> nodes = NodeIO.read("D:\\Programming\\Projects\\Oscar\\data.txt");
		for (ICourse gnode : nodes) {
			if (gnode instanceof PrereqGroup) {
				PrereqGroup group = (PrereqGroup)gnode;
				
				OdeNode gode = new OdeNode(gnode.getUID(), "*");
				mainManage.register(gode);
				
				for (String s : group.getChildren()) {
					mainManage.addParent(gode.getUID(), s);
				}
			} else {
				mainManage.register(new OdeNode(gnode.getUID(), gnode.getName()));
			}

			if (gnode instanceof Course) {
				Course node = (Course)gnode;
				for (String p : node.getParents())
					mainManage.addParent(node.getUID(), p);
			}
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
