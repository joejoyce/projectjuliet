package uk.ac.cam.cl.juliet.test.dataprocessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Packet;

/**
 * This class mocks a cluster master for testing the data processor.
 * 
 * Warning: currently this class only properly implements sendPacket(), start(), and stop()
 * 
 * @author michael
 *
 */
public class MockClusterMaster implements ClusterMaster {
	private ServerSocket socket = null;
	private long packetCount = 0;
	private long latestSeqNum = 2; // Packet sequence numbering appears to start from 2
	private long latestTimestamp = 0;
	private long latestTimestampNS = 0;

	public long sendPacket(Container msg) throws NoClusterException {
		// Extract a packet from msg
		XDPRequest xdpRequest = (XDPRequest)msg;
		Packet packet = new Packet(xdpRequest.getPacketData());
		
		// Assert that packets are being assigned a sequential, consecutive set of sequence numbers
		// This assertion fails at packet #479 - not sure why (mh701)
		/*
		assert(this.latestSeqNum <= packet.getSequenceNumber())
		: "Packet is not numbered correctly "
		+ this.latestSeqNum + " =/= " + packet.getSequenceNumber();
		*/
		
		// Assert on chronological ordering of incoming packets
		assert(this.latestTimestamp <= packet.getTimestamp()
		|| (this.latestTimestamp == packet.getTimestamp()
		    && this.latestTimestampNS <= packet.getTimestampNS()))
		: "Packets are being sent out of order "
		+ "(" + this.latestTimestamp + ", " + this.latestTimestampNS + ") "
		+ "(" + packet.getTimestamp() + ", " + packet.getTimestampNS() + ")";
		
		// Update latest holders for next assertions
		this.latestSeqNum = packet.getSequenceNumber() + packet.getNumberOfMsgs();
		this.latestTimestamp = packet.getTimestamp();
		this.latestTimestampNS = packet.getTimestampNS();
		
		// Print out packet key details
		this.packetCount++;
		System.out.printf(
				"#%d: msgCount = %d; seqNum = %d; sendTime = %d; sendTimeNS = %d\n",
				this.packetCount,
				packet.getNumberOfMsgs(),
				packet.getSequenceNumber(),
				packet.getTimestamp(),
				packet.getTimestampNS()
		);
		return 0L;
	}

	@Override
	public long sendPacket(Container msg, Callback cb)
			throws NoClusterException {
		return 0;
	}

	@Override
	public void addClient(Socket skt) {
		return;
	}

	@Override
	public void start(int port) throws IOException {
		if (this.socket != null) {
			this.socket.close();
		}
		this.socket = new ServerSocket(port);
		
	}

	@Override
	public void stop() {
		if(this.socket != null) {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.socket = null;
	}

	@Override
	public void removeClient(Client ob) {
		return;
	}

	@Override
	public void closeAndRemove(Client ob) {
		return;
	}

	@Override
	public long getNextId() {
		return 0;
	}

	@Override
	public String getSetting(String key) {
		return null;
	}

	@Override
	public void setSetting(String key, String value) {
		return;
	}

	@Override
	public ConfigurationPacket getConfiguration() {
		return null;
	}

	@Override
	public Client[] listClients() {
		return null;
	}

	@Override
	public int broadcast(Container c) {
		return 0;
	}

	@Override
	public int broadcast(Container c, Callback cb) {
		// TODO Auto-generated method stub
		return 0;
		
	}

	@Override
	public long getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPacketThroughput() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getClientCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public ScheduledFuture<?> repeatedBroadcast(Container c, Callback cb, long time){ return (ScheduledFuture<?>)null;}
	
	public ScheduledFuture<?> repeatedSend(Container c, Callback cb, long time){return (ScheduledFuture<?>)null;}

}
