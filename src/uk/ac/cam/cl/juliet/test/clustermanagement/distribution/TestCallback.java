package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
/**
 * A Test Callback that is used for testing the Clustermaster
 * It notifies its trackkeeper when the packet arrived back at the 
 * ClusterMaster.
 * 
 * @author Lucas Sonnabend
 *
 */
public class TestCallback extends Callback {
	private Trackkeeper tracker;
	/**
	 * Create a new TestCallback and give it an instance of a Trackkeeper
	 * @param pTracker 		The Trackkeeper it will use when the packet
	 * 						belonging to the callback arrives back at the 
	 * 						Clustermaster
	 */
	public TestCallback(Trackkeeper pTracker) {
		tracker = pTracker;
	}
	
	@Override
	public void callback(Container data) {
		MockXDPResponse response = (MockXDPResponse) data;
		int seq = response.getSequenceNo();
		tracker.ackPacketReturned(seq);
	}

}
