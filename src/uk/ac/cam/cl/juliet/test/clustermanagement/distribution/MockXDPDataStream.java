package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

import java.io.IOException;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;

public class MockXDPDataStream implements XDPDataStream {
	private long timeDifference_ns;
	private long timeOfNextPacket;
	private long accumulated_ns;
	private int noOfPackets;
	private int packetCounter;
	private int packetSize;
	private Trackkeeper tracker;
	/**
	 * create a new MockXDPDataStream that creates new packets at a specified
	 * rate. The size of each packet in bytes is defined by the packetSize. The data of the packet
	 * consists of a mostly empty byte array. Only the first four bytes are used
	 * to contain a unique sequence number to identify the packet throughout the system.
	 * A tracker object is notified whenever a packet is created.
	 * @param packetsPerSecond 		rate at which packets are created
	 * @param pNoOfPackets			total number of packets created over time
	 * @param pTracker				Trackkeeper that keeps track of the packets throughout the system.
	 * @param pPacketSize			Size of the packets in bytes that are created. 
	 * 								If the entered value is smaller than 50 or larger 
	 * 								than 1500 then a default packet size of 300 is used.
	 */
	public MockXDPDataStream(int packetsPerSecond, int pNoOfPackets,
			Trackkeeper pTracker, int pPacketSize) {
		if(packetsPerSecond <= 0) {
			System.err.println("invalid input! need a positive number of "
					+ "packets per second");
		}
		tracker = pTracker;
		timeDifference_ns = 
				Math.round((float) 1000000000 / (float) packetsPerSecond);
		timeOfNextPacket = 0;
		noOfPackets = pNoOfPackets;
		packetCounter = 0;
		accumulated_ns = 0;
		if(pPacketSize < 50 || pPacketSize > 1500) packetSize = 300;
		else packetSize = pPacketSize;
	}
	
	@SuppressWarnings("static-access")
	@Override
	public XDPRequest getPacket() throws IOException {
		if(packetCounter >= noOfPackets) {
			return null;
		}
		byte[] emptyPacketData = new byte[packetSize];
		enterSequenceNumber(emptyPacketData,packetCounter);
		XDPRequest packet = new XDPRequest(emptyPacketData, 11, 0);
		long currentTime = System.currentTimeMillis();
		try {
			if(currentTime <= timeOfNextPacket)
				Thread.currentThread().sleep(timeOfNextPacket - currentTime);
		} catch (InterruptedException e) {
			System.err.println("Problem with waiting before getting a new packet");
			e.printStackTrace();
		}
		
		//System.out.println("packet no "+packetCounter+" generated.");
		tracker.ackPacketGenerated(packetCounter);
	
		accumulated_ns += timeDifference_ns;
		if(accumulated_ns >= 500000) {
			// if the accumulated time difference is greater than half a millisecond
			// increase the time of the next packet by 1 ms and substract 1 ms from the
			// accumulated time in ns.
			accumulated_ns -= 1000000;
			timeOfNextPacket++;
		}
		this.packetCounter++;
		return packet;
	}
	
	private byte[] encodeIntAsByteArray(int i) {
		byte[] result = new byte[4];
		result[0] = (byte) (i & 0xFF);
		result[1] = (byte) ((i >>> 8) & 0xFF);
		result[2] = (byte) ((i >>> 16) & 0xFF);
		result[3] = (byte) ((i >>> 24) & 0xFF);
		return result;
	}
	
	private void enterSequenceNumber(byte[] array, int seq) {
		byte[] code = encodeIntAsByteArray(seq);
		array[0] = code[0];
		array[1] = code[1];
		array[2] = code[2];
		array[3] = code[3];
	}

	@Override
	public void setSkipBoundary(float pSkipBoundary) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getSkipBoundary() {
		// TODO Auto-generated method stub
		return 0;
	}

}
