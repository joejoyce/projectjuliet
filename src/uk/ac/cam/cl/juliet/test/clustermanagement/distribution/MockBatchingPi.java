package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.common.XDPResponse;
/**
 * This class is a fake raspberry Pi that can connect to a ClustMaster in order
 * to test the ClusterMaster.
 * 
 * @author Lucas Sonnabend
 *@see ClusterMasterLoadTest
 */
public class MockBatchingPi {
	private String name;
	private final Socket socket;
	private final ObjectInputStream input;
	private final ObjectOutputStream output;
	private BlockingQueue<Container> packetsToProcess;
	private KillableThread receiver;
	private Runnable processor;
	private static long batchInterval;
	private final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static final int RESET_LIMIT = 100000; 
	private int outputPacketCount = 0;
	
	/**
	 * Create a new MockPi and give it a name for testing/debugging reasons.
	 * It will connect to a ClusterMaster using an ObjectStream via server and port.
	 * On receiving a packet it will get the sequence number from the test XDPRequest,
	 * notify the tracker that the packet has arrived and wait the specified time.
	 * @param pName			name of the Pi
	 * @param server		address to connect to
	 * @param port			port to connect to
	 * @param waitingTime	time a Pi is supposed to wait to simulate processing,
	 * 						in milliseconds
	 * @param tracker		Trackkeeper to notify that the packet arrived
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MockBatchingPi(String pName, String server, int port, final long waitingTime,
			final Trackkeeper tracker) throws UnknownHostException, IOException {
		name = pName;
		socket = new Socket(server, port);
		output = new ObjectOutputStream(socket.getOutputStream());
		output.flush();
		input = new ObjectInputStream(socket.getInputStream());
		batchInterval = waitingTime;
		
		packetsToProcess = new LinkedBlockingQueue<Container>();
		
		receiver = new KillableThread() {
			@Override
			public void run() {
				while(alive) {
					try {
						Container c = (Container) input.readObject();
						packetsToProcess.put(c);
						
					} catch (ClassNotFoundException | IOException
							| InterruptedException e) {
						System.err.println(name+": error while receiving "
								+ "object from server");
						e.printStackTrace();
						return;
					}
				}
			}
			
		};
		receiver.start();
		
		processor = new Runnable() {
			@Override
			public void run() {
					try {
						while(!packetsToProcess.isEmpty()) {
							Container c = packetsToProcess.poll();
							if(c instanceof ConfigurationPacket) {
								System.out.println(name+": received configuration packet");
							} else if(c instanceof XDPRequest) {
								int seq = getPacketTestSequenceNumber(c);
								//System.out.println(seq +" arrived at pi "+name);
								tracker.ackPacketAtPi(seq);
								XDPResponse response = new MockXDPResponse(c.getPacketId(),
										true, seq);
								output.writeObject(response);
								outputPacketCount++;
								if(outputPacketCount >= RESET_LIMIT) {
									output.reset();
									outputPacketCount = 0;
									System.out.println("reset output stream");
								}
									
								
							}
						}
						output.flush();
					} catch (IOException e) {
						System.err.println(name+": could not send response");
						e.printStackTrace();
						return;
					}
				}
		};
		scheduler.scheduleAtFixedRate(processor, batchInterval, batchInterval, TimeUnit.SECONDS);
	}
	
	private int getPacketTestSequenceNumber(Container c) {
		XDPRequest packet = (XDPRequest) c;
		int sequenceNo = 0;
		sequenceNo += ((int)packet.getPacketData()[0]) & 0xFF;
		sequenceNo += (((int)packet.getPacketData()[1]) & 0xFF) << 8;
		sequenceNo += (((int)packet.getPacketData()[2]) & 0xFF) << 16;
		sequenceNo += (((int)packet.getPacketData()[3]) & 0xFF) << 24;
		return sequenceNo;
	}
	/**
	 * End the threads to receive packets and send responses and close
	 * the streams
	 * @throws IOException
	 */
	public void teardown() throws IOException {
		this.receiver.kill();
		this.output.close();
		this.input.close();
	}
	/**
	 * A thread that is easily killed.
	 * 
	 * @author Lucas Sonnabend
	 *
	 */
	private class KillableThread extends Thread {
		protected boolean alive = true;
		public void kill() {alive = false;}
	};
}
