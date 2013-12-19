package gm.nodeode.math;

public class TimeKeeper {
	
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
