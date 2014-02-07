package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.StringTestPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.common.XDPResponse;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnectionUnit;
import uk.ac.cam.cl.juliet.slave.queryprocessing.QueryProcessor;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;

/**
 * The listener reads packets send from the master PC and passes them on to
 * either the XDP processor or query handler.
 * 
 * @author Dylan McDermott
 * 
 */
public class Listener {
	private static final int numProcessingThreads = 4;

	private Socket socket;
	private Thread receiveThread;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private LinkedBlockingQueue<Container> responseQueue = new LinkedBlockingQueue<Container>();
	private LinkedBlockingQueue<Container> requestQueue = new LinkedBlockingQueue<Container>();
	private DatabaseConnection databaseConnection;
	private XDPProcessor xdp;
	private QueryProcessor query;
	private Thread[] processingThreads = new Thread[numProcessingThreads];

	/**
	 * Connects to the server and begins to read and process packets.
	 * 
	 * @param server
	 *            The server to connect to.
	 * @param port
	 *            The port which packets are being sent from.
	 * @throws IOException
	 */
	public void listen(String server, int port) throws IOException, SQLException {
		this.socket = new Socket(server, port);
		this.input = new ObjectInputStream(this.socket.getInputStream());
		this.output = new ObjectOutputStream(this.socket.getOutputStream());		
		
		this.databaseConnection = new DatabaseConnectionUnit(DriverManager.getConnection("jdbc:mysql://localhost:3306/juliet", "root", "rootword"));
		this.xdp = new XDPProcessorUnit(this.databaseConnection);
		// TODO Create query processor

		for (int i = 0; i < numProcessingThreads; i++) {
			this.processingThreads[i] = new Thread() {
				@Override
				public void run() {
					while (true) {
						processPacket();
					}
				}
			};
		}

		this.receiveThread = new Thread() {
			@Override
			public void run() {
				while (true)
					readPacket();
			}
		};
		this.receiveThread.start();

		// Sends any waiting responses back to the server.
		while (true) {
			try {
				Container response = this.responseQueue.poll();
				if(response == null) {
					System.out.println("null!!!");
					System.out.println("resqueue size: " + responseQueue.size());
					continue;
				}
				System.out.println("---- took respone off the queuu to send back: " + response.getPacketId());
				output.writeObject(response);
				System.out.println("---- Sending packet ack back for : " + response.getPacketId());
				output.flush();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void readPacket() {
		try {
			Container container = (Container) this.input.readObject();
			System.out.println("Got new object");
			if (container instanceof ConfigurationPacket) {
				handleConfigurationPacket((ConfigurationPacket) container);
			}
			else {
				this.requestQueue.add(container);
				System.out.println("added to requestQueue: " + container.getPacketId());
			}
		} catch (ClassNotFoundException | ClassCastException e) {
			System.err.println("An unexpected object was recieved from the server.");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void processPacket() {
		try {
			System.out.println("About to take of request queue");
			Container container = this.requestQueue.take();
			
			if (container instanceof XDPRequest) {
				processXDPRequest((XDPRequest) container);
			} else if (container instanceof QueryPacket) {
				processQueryPacket((QueryPacket) container);
			} else if (container instanceof StringTestPacket) {
				System.out.println(container);
			} else {
				System.out.println("Unknown packet");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// If the response queue is getting too big, send all responses
	/*	if (this.responseQueue.size() > 20) {
			synchronized (output) {
				try {
					Container response;
					while ((response = this.responseQueue.poll()) != null)
						output.writeObject(response);
					output.flush();
				} catch (IOException e) {
					System.err.println("An error occurred sending a response to the server");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}*/
	}

	private void processXDPRequest(XDPRequest container) {
		boolean result = this.xdp.decode(container);
		XDPResponse response = new XDPResponse(container.getPacketId(), result);
		try {
			this.responseQueue.put(response);			
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Added response: " + response.getPacketId() + ", to respoinsequeue, queue size: " + responseQueue.size());
	}

	private void processQueryPacket(QueryPacket container) {
		responseQueue.add(this.query.runQuery(container));
	}

	private void handleConfigurationPacket(ConfigurationPacket packet) {
		String ip = packet.getSetting("db.addr");
		if (ip != null) {
			try {
				this.databaseConnection.setConnection(DriverManager
						.getConnection("jdbc:mysql://" + ip + ":3306/juliet",
								"root", "rootword"));

			} catch (SQLException e) {
				System.err.println("An error occurred connecting to the database");
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (!this.processingThreads[0].isAlive()) {
			for (int i = 0; i < numProcessingThreads; i++)
				this.processingThreads[i].start();
		}
	}
}
