package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

import java.io.IOException;
import java.net.UnknownHostException;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;

public class ClusterMasterLoadTest {
	static MockPi[] mockPis;
	
	/**
	 * Runs a test with the given clustermaster and datastream.
	 * While there are still packets on the datastream, they are send using the
	 * clustermaster. A callback is provided that will run a method on the trackkeeper
	 * to note when the packet returned.
	 * @param cm		the clustermaster
	 * @param ds		the datastream
	 * @param tracker	Instance of the trackkeeper the callback will use
	 * @throws IOException
	 * @throws NoClusterException
	 */
	public static void run(ClusterMaster cm, XDPDataStream ds, Trackkeeper tracker)
			throws IOException, NoClusterException {
		Callback cb = new TestCallback(tracker);
		XDPRequest packet = ds.getPacket();
		while(packet != null) {
			//cm.sendPacket(packet);
			cm.sendPacket(packet, cb);
			packet = ds.getPacket();
		}
	}
	/**
	 * Runs a test for a ClusterMaster using the following parameters
	 * @param pNoOfPackets			total number of packets that are generated
	 * @param pPacketsPerSecond		rate at which packets are generated
	 * @param pWaitingTimeOfPis_ms	estimate of the time a Pi will need to process
	 * 								a single packet
	 * @param pNumberOfPis			number of simulated pis in the test
	 * @return						A string with a summary
	 */
	public static String runTest(int pNoOfPackets,int pPacketsPerSecond, 
			long pWaitingTimeOfPis_ms, int pNumberOfPis, int pPacketSize) {
		Trackkeeper myTracker = new Trackkeeper(pNoOfPackets);
		XDPDataStream ds = new MockXDPDataStream(pPacketsPerSecond, pNoOfPackets, 
				myTracker, pPacketSize);
		ClusterMaster cm = new ClusterMaster(""); // pass in the path of the configuration file
									// for now let an error be thrown and continue 
		//set up the clusterMaster to listen to new Pis
		try {
			cm.start(5001);
			addLocalMockPisToClusterMaster(pNumberOfPis, pWaitingTimeOfPis_ms, 
					5001, myTracker);
		} catch (IOException e) {
			e.printStackTrace();
			return "could not start cluster master or connect Pis"
					+ "to the cluster master!";
			
		}
		long startTime = System.currentTimeMillis();
		try {
			run(cm, ds, myTracker);
			while(!myTracker.haveAllReturned()) { 
				Thread.sleep(100);
			}
		} catch (IOException | NoClusterException e) {
			e.printStackTrace();
			return "Error while running! This means either an error"
					+ "in getting the next packet or in sending a packet";
		} catch (InterruptedException e) {
			return "Error while waiting for Pis to finish.";
		}
		
		long stopTime = System.currentTimeMillis();
		long executionTime = (long) ((float)(stopTime-startTime) / 1000f);
		
		/* this throws a nice stack-trace because thats what the client does at the 
		 * moment when it loses the connection to the pi
		try {
			removePis();
		} catch (IOException e) {
			System.err.println("Error while trying to tear down the pis!");
			e.printStackTrace();
		}
		*/
		
		return "The system needed "+executionTime+"s to send "+
				pNoOfPackets+" at a rate of "+pPacketsPerSecond+" packets per "
						+ "second to "+pNumberOfPis+" pis!";
	}
	/**
	 * Starts a test with the provided arguments
	 * 		Usage: <No. of test packets to send> <Packets per second> 
	 *			   <processing time at pi> <number of Pis>
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			int noOfPackets = Integer.parseInt(args[0]);
			int packetsPerSecond = Integer.parseInt(args[1]);
			long waitingTimeOfPi_ms = Long.parseLong(args[2]);
			int numberOfPis = Integer.parseInt(args[3]);
			
			String output = runTest(noOfPackets, packetsPerSecond, 
					waitingTimeOfPi_ms, numberOfPis, 500);
			System.out.println(output);
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("Usage: <No. of test packets to send> <Packets per second> "
					+ "<processing time at pi> <number of Pis>");
			return;
		}
		
	}
	/**
	 * create instances of MockPis used for the testing. They are then stored
	 * in a static global variable
	 * @param number		number of MockPis to be created
	 * @param waitingTime	estimated processing time a MockPi shall simulate
	 * @param port			port of the ClusterMaster
	 * @param tracker		instance of a Trackkeeper the Pis shall use to indicate
	 * 						that a packet arrived
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static void addLocalMockPisToClusterMaster(int number, long waitingTime,
			int port, Trackkeeper tracker) throws UnknownHostException, IOException {
		mockPis = new MockPi[number];
		for(int i = 0; i<number;i++) {
			mockPis[i] = new MockPi("Pi"+i, "localhost", port, waitingTime, 
					tracker);
			System.out.println("added a pi: "+number+"!");
		}
	}
	
	/**
	 * should be called after a test has been run to end the connections
	 * @throws IOException
	 */
	private static void removePis() throws IOException {
		for(MockPi pi : mockPis) {
			pi.teardown();
		}
	}
}
