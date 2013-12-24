package gm.nodeode.math;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Convenience methods for measuring system time
 * @author Garrett
 *
 */
public class TimeKeeper {
	
	private static ConcurrentHashMap<String, Long> ticks = new ConcurrentHashMap<String, Long>();
	
	public static void tick(String label) {
		ticks.put(label, System.currentTimeMillis());
	}
	public static long tock(String label) {
		if (!ticks.containsKey(label))
			return 0;
		
		return System.currentTimeMillis() - ticks.get(label);
	}
	
	public static void tick() {
		tick(Thread.currentThread().getName());
	}
	public static long tock() {
		return tock(Thread.currentThread().getName());
	}
	public static long atock(String label) {
		long result = tock(label);
		System.out.println(label + ": " + result + " ms");
		return result;
	}
	public static long atock() {
		return atock(Thread.currentThread().getName());
	}
	
	private long msstart = -1;
	private long msend = -1;
	private long mspass = 0;
	private float seconds = 0;
	
	public synchronized float timePassed() {
		msend = System.currentTimeMillis();
		if (msstart < 0 || msend < 0) {
			mspass = 0;
			seconds = 0;
		} else {
			mspass = msend - msstart;
			seconds = mspass / 1000f;
		}
		msstart = System.currentTimeMillis();
		
		return seconds;
	}
	
	public float timePassedSinceStartup() {
		if (msstart < 0) return 0;
		return (System.currentTimeMillis() - msstart) / 1000f;
	}
}
