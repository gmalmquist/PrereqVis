package gm.nodeode.io;

import java.util.Arrays;

public class PrereqGroup implements ICourse {

	private String[] children;
	private String id;
	private String name;
	
	public PrereqGroup(String[] children) {
		this.children = children;
		this.id = join(",", children);
		this.name = join("|", children);
	}

	public String[] getChildren() {
		return children;
	}
	
	public int getType() {
		return Course.TYPE_GROUP;
	}
	
	private static String join(String j, String[] s) {
		StringBuffer sb = new StringBuffer(s.length*10);
		for (String ss : s) {
			if (sb.length() > 0)
				sb.append(j);
			sb.append(ss);
		}
		return sb.toString();
	}

	@Override
	public String getUID() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}
}
