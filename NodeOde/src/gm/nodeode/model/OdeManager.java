package gm.nodeode.model;

import java.util.Collection;


public interface OdeManager {
	public Visode find(String id);
	public void register(Visode ode);
	public void remove(Visode ode);
	
	public void addParent(String ode, String parent);
	
	public Iterable<String> getParents(String ode);
	
	public boolean hasParents(String ode);
	public boolean hasChildren(String ode);
	
	public Collection<String> getOdes();
}
