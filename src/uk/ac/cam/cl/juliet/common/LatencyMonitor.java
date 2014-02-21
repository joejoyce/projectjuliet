package uk.ac.cam.cl.juliet.common;

public class LatencyMonitor extends Container {
	public long outboundQueue;
	public long outboundDepart;
	public long outboundArrive;
	public long outboundDequeue;
	public long databaseRoundTrip;
	public long inboundQueue;
	public long inboundDepart;
	public long inboundArrive;
	public String addr = null;
}
