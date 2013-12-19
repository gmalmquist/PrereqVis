package gm.nodeode.io;

import java.util.LinkedList;
import java.util.List;

public class Course implements ICourse {
	private String name;
	private String fullname;
	private List<String> tags;
	private List<String> parents;
	
	public static final int TYPE_NORMAL = 0;
	public static final int TYPE_GROUP = 1;
	public static final int TYPE_BLANK = 2;
	
	public Course(String name, String fullname, String[] tags) {
		this.name = name;
		this.fullname = fullname;
		this.parents = new LinkedList<String>();
		this.tags = new LinkedList<String>();
		for (String s : tags)
			this.tags.add(s);
	}
	
	public int getType() {
		return TYPE_NORMAL;
	}
	
	public String getFullName() {
		return fullname;
	}
	
	public void addParent(ICourse node) {
		parents.add(node.getUID());
	}
	
	public void addParent(String node) {
		parents.add(node);
	}
	
	public Iterable<String> getParents() {
		return parents;
	}

	@Override
	public String getUID() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}
	
}
