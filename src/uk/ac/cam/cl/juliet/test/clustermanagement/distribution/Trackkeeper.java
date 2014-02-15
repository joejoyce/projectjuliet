package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

public class Trackkeeper {
	private boolean[] isGenerated;
	private boolean[] arrivedAtPi;
	private boolean[] arrivedBack;
	private int size;
	
	/**
	 * Generates a new trackkeeper that can keep account of where a packet with a 
	 * certain sequence number is in the system.
	 * @param size - number of packets it can keep track of
	 */
	public Trackkeeper(int pSize) {
		size = pSize;
		isGenerated = new boolean[size];
		arrivedAtPi = new boolean[size];
		arrivedBack = new boolean[size];
		for(int i=0;i<size;i++) {
			isGenerated[i] = false;
			arrivedAtPi[i] = false;
			arrivedBack[i] = false;
		}
	}
	/**
	 * Tell the trackkeeper that a packet of sequence number n has been generated
	 * @param n		sequence number
	 */
	public void ackPacketGenerated(int n) {
		isGenerated[n] = true;
	}
	/**
	 * Tell the trackkeeper that a packet with sequence number n has arrived at a pi
	 * @param n		sequence number
	 */
	public void ackPacketAtPi(int n) {
		arrivedAtPi[n] = true;
	}
	/**
	 * Tell the trackkeeper that a packet with sequence number n has arrived 
	 * back at the clusterMaster
	 * @param n		sequence number
	 */
	public void ackPacketReturned(int n) {
		arrivedBack[n] = true;
	}
	/**
	 * check whether all packets arrived at a Pi
	 * @return true iff all packets arrived at a pi
	 */
	public boolean haveAllArrivedAtPis() {
		boolean result = true;
		for(int i = size-1; result && i >= 0; i--) {
			result &= isGenerated[i] & arrivedAtPi[i];
		}
		return result;
	}
	/**
	 * check whether all packets arrived back at the 
	 * clusterMaster.
	 * @return true if all packets arrived back
	 */
	public boolean haveAllReturned() {
		boolean result = true;
		for(int i = size-1; result && i >= 0; i--) {
			result &= isGenerated[i] & arrivedBack[i];
		}
		return result;
	}
}
