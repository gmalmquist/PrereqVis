package gm.nodeode.model;

public abstract class OdeLayout {
	protected OdeAccess db;
	
	public OdeLayout(OdeAccess db) {
		this.db = db;
	}
	
	public abstract void doLayout();
	
	public abstract void doLayoutStep();
}
