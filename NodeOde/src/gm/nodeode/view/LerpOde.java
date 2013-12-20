package gm.nodeode.view;

import gm.nodeode.math.geom.Mathf;
import gm.nodeode.math.geom.Pt;
import gm.nodeode.model.Visode;

public class LerpOde {
	private Pt start;
	private Pt end;
	private Visode on;
	private float duration;
	private float time;
	private boolean done;
	
	public LerpOde(Visode on, Pt start, Pt end, float duration) {
		this.start = start;
		this.end = end;
		this.on = on;
		this.duration = duration;
		
		this.time = 0;
		this.done = false;
	}
	public LerpOde(Visode on, Pt start, Pt end) {
		this(on, start, end, 1);
	}
	
	public void update(float time) {
		if (isDone())
			return;
		
		if (this.time + time >= this.duration) {
			finish();
		} else {
			this.time += time;
			on.setCenter((Pt) Mathf.lerp(start, end, this.time / this.duration));
		}	
	}
	
	public void finish() {
		on.setCenter(end);
		this.done = true;
		this.time = this.duration;
	}
	
	public boolean isDone() {
		return done;
	}
}
