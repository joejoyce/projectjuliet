package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.SpikeDetectionRequest;
import uk.ac.cam.cl.juliet.common.SpikeDetectionResponse;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;
/**
 * Class for a background thread that will repeatedly ask a Pi to check for spikes
 * in the trades
 * @author lucas
 *
 */
public class SpikeDetectionRunnable implements Runnable{
	private int sleepingTime;
	private ClusterMaster clusterMaster;
	private boolean isRunning;
	private int secondsToLookInPast;
	private float limit;
	private boolean requestOnItsWay;
	/**
	 * we know for sure that no spike had occurred before the checkpoint time:
	 */
	private long checkpointTime;
	/**
	 * Creates a SpikeDetecionRunnable that shall be run as a separate thread.
	 * It will, when started, repeatedly send a SpikeDetectionRequest via the 
	 * clusterMaster to find detected spikes.
	 * A spike is detected by looking at the average trading price for each stock in
	 * the last 'secondsToLookInPast' seconds and return an alert to the webserver
	 * whenever a trade differs from the average by more then (limit*100)%.
	 * In between requests it will wait some minimum time, and it will never send
	 * a request before the previous one has returned.
	 * @param cm	clusterMaster to connect to a cluster
	 * @param sleepingTime	minimum time in milliseconds it will wait between requests 
	 * @param secondsToLookInPast	time in seconds to look into the past for taking the average
	 * 						(for example 1800 will consider the last half an hour)
	 * @param limit		The limit by which a stock has to differ from the average
	 * 					to be considered a spke
	 */
	public SpikeDetectionRunnable(ClusterMaster cm, int sleepingTime,
			int secondsToLookInPast, int limit) {
		this.sleepingTime = sleepingTime;
		this.clusterMaster = cm;
		this.secondsToLookInPast = secondsToLookInPast;
		this.limit = limit;
		this.isRunning = false;
		this.checkpointTime = 0;
	}
	
	@Override
	public void run() {
		this.isRunning = true;
		while(this.isRunning) {
			//check whether another request has not yet returned, if so wait for it
			while(this.requestOnItsWay){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Debug.println(Debug.ERROR, "SpikeDetectionRunnable: "
							+ "InterruptedException while trying to sleep.");
				}
			}
				
			//send out a request to check for spikes to a Pi
			long currentTime = clusterMaster.getTime();
			long startingTime = currentTime -secondsToLookInPast;
			if(checkpointTime < startingTime) checkpointTime = startingTime;
			SpikeDetectionRequest query = new SpikeDetectionRequest(
					startingTime, checkpointTime, limit);
			try {
				clusterMaster.sendPacket(query, new SpikeDetectionCallback());
			} catch (NoClusterException e) {
				Debug.println(Debug.ERROR, "Cluster query exception while "
						+ "asking for spike detection");
			}
			this.requestOnItsWay = true;
			//update checkpoint time
			this.checkpointTime = currentTime;
			
			// sleep before you you start the next query
			try {
				Thread.sleep(this.sleepingTime);
			} catch (InterruptedException e) {
				Debug.println(Debug.ERROR, "SpikeDetectionRunnable: "
						+ "InterruptedException while trying to sleep.");
			}
			
		}
	}
	
	/**
	 * check whether the spike detection is running or not
	 * If it is
	 * @return
	 */
	public boolean isRunning() {return this.isRunning;}
	/**
	 * Turn the spike detection on or off
	 * @param running
	 */
	public void setRunning(boolean running) {
		if(!this.isRunning && running)
			this.run();
		this.isRunning = running;
		}
	
	private class SpikeDetectionCallback implements Callback {

		@Override
		public void callback(Container data) {
			SpikeDetectionResponse response = (SpikeDetectionResponse) data;
			// iterate through all the recorded spikes
			long[] times = response.getSpikyTimes();
			String[] symbol = response.getSpikySymbols();
			for(int i = 0; i< response.getNumberOfSpikes(); i++) {
				//TODO bring it into a form so that the web-server can use it
				
				// symbol of a spike: symbol[i]
				// time of this same spike: times[i]
				
				// Because the query can be delayed between issuing on the master side
				// and the database access on the Pi side, we could detect spikes that
				// happened after the checkpoint time. Therefore update the checkpoint
				// time in case this happened
				if(checkpointTime < times[i]) {
					checkpointTime = times[i];
				}
			}
			requestOnItsWay = false;	
		}
	}
}
