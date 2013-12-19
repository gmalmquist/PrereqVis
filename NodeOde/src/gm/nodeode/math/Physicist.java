package gm.nodeode.math;

import java.util.HashMap;

public class Physicist<Particle> {
	private CircleAdapter<Particle> adapter;
	private HashMap<Particle, Pt> 	momentum;
	private HashMap<Particle, Pt> 	forces;
	private Iterable<Particle> 		particles;
	
	// Following the punnic naming convention
	private final Object swimming = new Object();
	
	public Physicist(CircleAdapter<Particle> adapter, Iterable<Particle> particles) {
		this.adapter = adapter;
		this.particles = particles;
		
		this.momentum = new HashMap<Particle, Pt>();
		this.forces = new HashMap<Particle, Pt>();
	}
	
	private Pt momentum(Particle p) {
		if (!momentum.containsKey(p))
			momentum.put(p, Pt.P(0,0));
		return momentum.get(p);
	}
	private Pt force(Particle p) {
		if (!forces.containsKey(p))
			forces.put(p, Pt.P(0,0));
		return forces.get(p);
	}
	
	/**
	 * Advects the particle as far as it can without collision,
	 * and returns the remaining travel vector
	 * @param p
	 * @param velocity
	 * @param time
	 * @return
	 */
	private Pt advect(Particle p, Pt velocity, float time, boolean bounce) {
		if (Float.isNaN(velocity.mag2()) || Float.isInfinite(velocity.mag2())) {
			return Pt.P(0,0);
		}
		
		float maxdisp = 10;
		if (velocity.mag2()*time*time > maxdisp*maxdisp) {
			// TOO FAST
			velocity.mul(maxdisp/(time*velocity.mag()));
		}
		
		Particle ignore = null;
		float duration = time;
		do {
			time = duration;
			Particle collider = null;
			
			for (Particle o : particles) {
				if (o.equals(p) || (ignore != null && o.equals(ignore)))
					continue;
				
				if (adapter.position(o).sub(adapter.position(p)).dot(velocity) < 0)
					continue; // no future collision 
				
				float toc = Mathf.circleCircleCollision(
						adapter.position(p), adapter.radius(p), velocity, 
						adapter.position(o), adapter.radius(o));
				
				if (Float.isNaN(toc) || Float.isInfinite(toc))
					continue; // no collision
				
				if (toc < 0) continue; // negative collision
				
				if (toc > time) continue; // To far in the future
				
				collider = o;
				time = toc;
			}
			
			Pt displacement = velocity.d().mul(time * 0.99f);
			adapter.moveBy(p, displacement);
			
			if (collider != null) {
				// reflection time
				momentum(collider).add(adapter.mass(p), velocity);
				velocity = velocity.d().mul(-1f);
				ignore = collider;
//				velocity = Mathf.reflect(velocity, adapter.position(p).d().sub(adapter.position(collider))).mul(0.5f);
			}
			
			duration -= time;
		} while (bounce && duration > 0);
		
		return velocity;
	}
	
	private float dragMagnitude(Particle p) {
		// 1/2 pv^2 Cd A
		return 0.5f * momentum(p).mag2() / Mathf.sq(adapter.mass(p)) * 0.1f * (2 * adapter.radius(p));
	}
	
	private void applyDrag(Particle p, float time) {
		Pt dir = momentum(p);
		if (dir.mag2() == 0)
			return;
		
		force(p).add(-dragMagnitude(p) / dir.mag(), dir);
	}
	
	public void applyForce(Particle p, Pt force, float time) {
		synchronized (swimming) {
			force(p).add(time, force);
		}
	}
	public void applyImpulse(Particle p, Pt impulse) {
		applyForce(p, impulse, 1);
	}
	
	public void update(float time) {
		synchronized (swimming) { // hehehe
			for (Particle p : particles) {
//				System.out.println("Apply drag");
				applyDrag(p, time);
				
				Pt M = momentum(p);
				M.add(time, force(p));
				
//				System.out.println("Advect");
				Pt velocity = M.d().mul(1f/adapter.mass(p));
				M.set(advect(p, velocity, time, false).mul(adapter.mass(p)));
			}
			
			forces.clear();
		}
	}
}
