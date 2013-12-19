package gm.nodeode.view;

import gm.nodeode.math.GP;
import gm.nodeode.math.Mathf;
import gm.nodeode.math.Pt;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class Art {
	public static void line(Graphics2D g, GP A, GP B) {
		g.drawLine(A.ix(), A.iy(), B.ix(), B.iy());
	}
	
	public static void lines(Graphics2D g, GP ... pts) {
		GP l = null;
		for (GP p : pts) {
			if (l != null)
				line(g, l, p);
			l = p;
		}
	}
	public static void show(Graphics2D g, GP P, float radius) {
		show(g, P, radius, null);
	}
	
	public static void arrow(Graphics2D g, GP A, GP B) {
		arrow(g, A, B, false);
	}
	public static void arrow(Graphics2D g, GP A, GP B, boolean taperStyle) {
		Pt rot = Pt.P(0,0).add(A).sub(B);
		
		if (taperStyle) {
			Pt T = rot.d().normalize();
			Pt N = T.d().rot2d();
			float s0 = 10;
			float s1 = 2;
			
			Pt[] pts = {
				N.d().mul(+s0).add(A).add(s0, T),
				N.d().mul(+s1).add(B),
				N.d().mul(-s1).add(B),
				N.d().mul(-s0).add(A).add(s0, T)
			};
			
			int[] x = new int[pts.length];
			int[] y = new int[pts.length];
			for (int i = 0; i < pts.length; i++) {
				x[i] = pts[i].ix();
				y[i] = pts[i].iy();
			}

			Color c = g.getColor();
			g.setColor(Color.GRAY);
			g.fillPolygon(x, y, pts.length);
			g.setColor(c);
			g.drawPolygon(x, y, pts.length);
		} else {
			line(g, A, B);
			
			float m = rot.mag();
			float l = 10f;//Mathf.max(10f, m/10f);
			rot.mul(l/m);
			
			line(g, B, rot.d().rot2d( Mathf.PI*1/5).add(B));
			line(g, B, rot.d().rot2d(-Mathf.PI*1/5).add(B));
		}
	}
	
	public static void show(Graphics2D g, GP P, float radius, String label) {
		g.drawOval(
			(int)(P.x() - radius),
			(int)(P.y() - radius),
			(int)(radius*2),
			(int)(radius*2)
		);
		
		if (label == null)
			return;
		
		FontMetrics fm = g.getFontMetrics();
		
		g.drawString(label, P.ix() - fm.stringWidth(label)/2, P.iy()+fm.getAscent()-fm.getHeight()/2);
	}
}
