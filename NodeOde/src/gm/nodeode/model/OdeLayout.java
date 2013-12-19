package gm.nodeode.model;

public abstract class OdeLayout {
	protected OdeManager db;
	
	public OdeLayout(OdeManager db) {
		this.db = db;
	}
	
	public abstract void doLayout();
	
	public abstract void doLayoutStep();
}
