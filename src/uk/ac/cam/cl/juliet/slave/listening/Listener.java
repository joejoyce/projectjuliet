package uk.ac.cam.cl.juliet.slave.listening;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.StringTestPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.common.XDPResponse;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.queryprocessing.QueryProcessor;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;

/**
 * The listener reads packets send from the master PC and passes them on to
 * either the XDP processor or query handler.
 * 
 * @author Dylan McDermott
 * 
 */
public class Listener {
	private Socket socket;
	private String ip;
	private int port;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private ArrayBlockingQueue<Container> responseQueue = new ArrayBlockingQueue<Container>(200);
	private DatabaseConnection databaseConnection;
	private XDPProcessor xdp;
	private QueryProcessor query;
	
	/**
	 * Connects to the server and begins to read and process packets.
	 * 
	 * @param server
	 *            The server to connect to.
	 * @param port
	 *            The port which packets are being sent from.
	 * @throws IOException
	 */
	public void listen(String server, int port, DatabaseConnection db, XDPProcessor xdpProcessor, QueryProcessor queryProcessor) throws IOException, SQLException {
		this.ip = server;
		this.port = port;
		this.socket = new Socket(server, port);
		this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		output.flush();
		this.input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		
		this.databaseConnection = db;
		this.xdp = xdpProcessor;
		this.query = queryProcessor;

		Thread receiveThread = new Thread() {
			@Override
			public void run() {
				while (true)
					readPacket();
			}
		};
		receiveThread.start();
		
		// Sends any waiting responses back to the server.
		while (true) {
			try {
				Container response = responseQueue.take();
				output.writeObject(response);
				output.flush();
				Debug.println("sent: size: " + responseQueue.size());
			} catch (IOException e) {
				e.printStackTrace();
				// Just attempt to reconnect
				try {
					socket = new Socket(ip, port);
					output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
					output.flush();
					input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	private void readPacket() {
		try {
			Container container = (Container) input.readObject();
			Debug.println("Got new object");
			
			if (container instanceof ConfigurationPacket) {
				handleConfigurationPacket((ConfigurationPacket) container);
			}
			else {
				long then = System.nanoTime();
				
				if (container instanceof XDPRequest) {
					processXDPRequest((XDPRequest) container);
				} else if (container instanceof QueryPacket) {
					processQueryPacket((QueryPacket) container);
				} else if (container instanceof StringTestPacket) {
					System.out.println(container);
				} 
				
				long diff = Math.abs(System.nanoTime() - then);
				diff /= 1000000;
				Debug.println("Time taken for processing ms: " + diff);				
			}
		} catch (ClassNotFoundException | ClassCastException e) {
			System.err.println("An unexpected object was recieved from the server.");
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			// Just attempt to reconnect
			try {
				this.socket = new Socket(ip, port);
				this.input = new ObjectInputStream(this.socket.getInputStream());
				this.output = new ObjectOutputStream(this.socket.getOutputStream());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void processXDPRequest(XDPRequest container) {
		boolean result = this.xdp.decode(container);
		XDPResponse response = new XDPResponse(container.getPacketId(), result);
		while (true) {
			try {
				responseQueue.put(response);
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void processQueryPacket(QueryPacket container) {
		while (true) {
			try {
				responseQueue.put(this.query.runQuery(container));
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleConfigurationPacket(ConfigurationPacket packet) {
		String ip = packet.getSetting("db.addr");
		if (ip != null) {
			/*
			 * try {
			 * this.databaseConnection.setConnection(DriverManager.getConnection
			 * ("jdbc:mysql://" + ip + ":3306/juliet", "root", "rootword"));
			 * 
			 * } catch (SQLException e) {
			 * System.err.println("An error occurred connecting to the database"
			 * ); e.printStackTrace(); System.exit(1); }
			 */
		}		
	}
}
