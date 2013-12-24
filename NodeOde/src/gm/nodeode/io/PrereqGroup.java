package gm.nodeode.io;

import java.util.Arrays;

/**
 * Essentially a virtual course which contains a tree of prerequisite logic
 * @author Garrett
 *
 */
public class PrereqGroup implements ICourse {

	private String[] children;
	private String id;
	private String name;
	private boolean and;
	
	public PrereqGroup(String[] children, boolean and) {
		this.children = children;
		this.id = "("+join(and ? "&" : "|", children)+")";
		this.name = join(and ? "&" : "|", children);
		this.and = and;
	}
	
	public boolean isAnd() {
		return and;
	}
	public boolean isOr() {
		return !and;
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
	
	@Override
	public String toString() {
		return getUID();
	}
}
