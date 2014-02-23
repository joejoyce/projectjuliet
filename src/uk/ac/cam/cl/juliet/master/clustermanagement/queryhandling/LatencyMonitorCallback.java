package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.LatencyMonitor;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;

public class LatencyMonitorCallback extends Callback{
	private long timeoutStamp;
	
	private int numBack = 0;
	long AvMasterOBQTime;
	long AvClientOBQTime;
	long AvDatabaseRTTime;
	long AvClientIBQTime;
	long AvNetworkRTTime;
	
	public LatencyMonitorCallback(long seconds) {
		timeoutStamp = System.nanoTime() + (seconds * 1000000000L);
	}
	public boolean isDone() {
		return(timeoutStamp <= System.nanoTime());
	}
	
	private long average(long av, long nVal) {
		return (av * numBack + nVal) / (numBack + 1);
	}
	@Override
	public synchronized void callback(Container data) {
		if(data instanceof LatencyMonitor) {
			LatencyMonitor m = (LatencyMonitor) data;
			AvMasterOBQTime = average(AvMasterOBQTime,m.outboundDepart - m.outboundQueue);
			AvClientOBQTime = average(AvClientOBQTime,m.outboundDequeue - m.outboundArrive);
			AvDatabaseRTTime = average(AvDatabaseRTTime,m.databaseRoundTrip);
			AvClientIBQTime = average(AvClientIBQTime,m.inboundDepart - m.inboundQueue);
			AvNetworkRTTime = average(AvNetworkRTTime, m.outboundDepart - m.outboundArrive + m.inboundArrive - m.inboundDepart);
			numBack++;
		} else {
			Debug.println(Debug.ERROR,"LatencyMonitor callback invoked on wrong container type");
		}
		
	}
	
	public String generateJson() {
		JsonBuilder jb = new JsonBuilder();
		jb.stOb();
		jb.pushPair("masterOBQTime", AvMasterOBQTime);
		jb.pushPair("clientOBQTime", AvClientOBQTime);
		jb.pushPair("databaseRTTime", AvDatabaseRTTime);
		jb.pushPair("clientIBQTime", AvClientIBQTime);
		jb.pushPair("networkRTTime", AvNetworkRTTime);
		jb.finOb();
		return jb.toString();
	}

}
