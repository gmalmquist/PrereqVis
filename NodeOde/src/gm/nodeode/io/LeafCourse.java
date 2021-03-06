package gm.nodeode.io;

/**
 * Course which has no prereqs
 * @author Garrett
 *
 */
public class LeafCourse implements ICourse {
	private String name;
	public LeafCourse(String name) {
		this.name = name;
	}
	
	public String getUID() {
		return name;
	}
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return "EmptyNode[" + name + "]";
	}
}
