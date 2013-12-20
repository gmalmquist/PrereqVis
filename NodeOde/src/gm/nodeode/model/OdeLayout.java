package gm.nodeode.model;

import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;

public abstract class OdeLayout {
	protected OdeAccess db;
	
	public OdeLayout(OdeAccess db) {
		this.db = db;
	}
	
	public abstract void doLayout();
	
	public abstract void doLayoutStep();
	
	private Pt V(Pt a, Pt b) {
		return b.d().sub(a);
	}
	public boolean edgesCross(String a0, String a1, String b0, String b1) {
		if (a0.equals(b0) || a0.equals(b1)) return false;
		if (a1.equals(b0) || a1.equals(b1)) return false;
		
		Pt A0 = db.find(a0).getCenter();
		Pt A1 = db.find(a1).getCenter();
		
		Pt B0 = db.find(b0).getCenter();
		Pt B1 = db.find(b1).getCenter();

		Pt TA = V(A0, A1).normalize();
		Pt TB = V(B0, B1).normalize();

		A0.add(+db.find(a0).radius(), TA);
		A1.add(-db.find(a1).radius(), TA);

		B0.add(+db.find(b0).radius(), TB);
		B1.add(-db.find(b1).radius(), TB);
		
		Pt N = TA.d().cross(TB);
		
		Pt NA = N.d().cross(TA);
		Pt NB = N.d().cross(TB);

		if (Mathf.sign(NA.dot(V(A0,B0))) == Mathf.sign(NA.dot(V(A0, B1))))
			return false;
		if (Mathf.sign(NB.dot(V(B0,A0))) == Mathf.sign(NB.dot(V(B0, A1))))
			return false;
		
		return true;
	}
}
