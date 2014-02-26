package uk.ac.cam.cl.juliet.slave.listening;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.LatencyMonitor;
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
	private ArrayBlockingQueue<Container> responseQueue = new ArrayBlockingQueue<Container>(2000);
	private ArrayBlockingQueue<Container> receiveQueue = new ArrayBlockingQueue<Container>(2000);
	private LinkedList<XDPRequest> waitingForBatchQueries = new LinkedList<XDPRequest>();
	private ReentrantLock waitingForBatchQueriesLock = new ReentrantLock();
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
	public void listen(String server, int thePort, DatabaseConnection db, XDPProcessor xdpProcessor, QueryProcessor queryProcessor) throws IOException, SQLException {
		this.ip = server;
		this.port = thePort;
		this.socket = new Socket(server, thePort);
		this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		output.flush();
		this.input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

		this.databaseConnection = db;
		this.xdp = xdpProcessor;
		this.query = queryProcessor;
		this.databaseConnection.addBatchQueryExecuteStartCallback(new Runnable() {
			@Override
			public void run() {
				// Stop packets from being added to
				// waitingForBatchQueries
				waitingForBatchQueriesLock.lock();
			}
		});
		this.databaseConnection.addBatchQueryExecuteEndCallback(new Runnable() {
			@Override
			public void run() {
				flushWaitingForBatchQueries();
				// Allow packets to be added to waitingForBatchQueries
				waitingForBatchQueriesLock.unlock();
			}
		});

		Thread readThread = new Thread() {
			@Override
			public void run() {
				while (true)
					readPacket();
			}
		};
		readThread.start();

		// NEW BIT TO RE-THREAD CLIENT
		Thread receiveThread = new Thread() {
			public void run() {
				while (true) {
					Object o;
					try {
						o = input.readObject();
						if (o instanceof Container) {
							if (o instanceof LatencyMonitor) {
								((LatencyMonitor) o).outboundArrive = System.nanoTime();
							}
							receiveQueue.put((Container) o);
						} else
							Debug.println(Debug.ERROR, "Unrecognised object type");
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						System.err.println("An error occurred communicating with the server.");
						e.printStackTrace();
						// Just attempt to reconnect
						try {
							socket = new Socket(ip, port);
							input = new ObjectInputStream(socket.getInputStream());
							output = new ObjectOutputStream(socket.getOutputStream());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		};
		receiveThread.start();

		// Sends any waiting responses back to the server.
		while (true) {
			try {
				Container response = responseQueue.take();
				if (response instanceof LatencyMonitor) {
					LatencyMonitor m = (LatencyMonitor) response;
					m.inboundDepart = System.nanoTime();
				}
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

	private void flushWaitingForBatchQueries() {
		XDPResponse response = null;
		while (waitingForBatchQueries.peek() != null) {
			XDPRequest r = waitingForBatchQueries.pop();
			response = new XDPResponse(r.getPacketId(), true);
			try {
				responseQueue.put(response);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void readPacket() {
		try {
			Container container = receiveQueue.take();

			Debug.println("Got new object");

			if (container instanceof ConfigurationPacket) {
				handleConfigurationPacket((ConfigurationPacket) container);
			} else if (container instanceof LatencyMonitor) {
				LatencyMonitor m = (LatencyMonitor) container;
				handleLatencyMonitor(m);
			} else {
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
		} /*
		 * catch (ClassNotFoundException | ClassCastException e) {
		 * System.err.println
		 * ("An unexpected object was recieved from the server.");
		 * e.printStackTrace(); System.exit(0); } catch (IOException e) {
		 * System.
		 * err.println("An error occurred communicating with the server.");
		 * e.printStackTrace(); // Just attempt to reconnect try { this.socket =
		 * new Socket(ip, port); this.input = new
		 * ObjectInputStream(this.socket.getInputStream()); this.output = new
		 * ObjectOutputStream(this.socket.getOutputStream()); } catch (Exception
		 * ex) { ex.printStackTrace(); } }
		 */
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void processXDPRequest(XDPRequest container) {
		boolean result = this.xdp.decode(container);
		if (result == false) {
			XDPResponse response = new XDPResponse(container.getPacketId(), false);
			while (true) {
				try {
					responseQueue.put(response);
					return;
				} catch (InterruptedException e) {
				}
			}
		} else {
			waitingForBatchQueriesLock.lock();
			waitingForBatchQueries.add(container);
			waitingForBatchQueriesLock.unlock();
			return;
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

	private void handleLatencyMonitor(LatencyMonitor m) {
		m.outboundDequeue = System.nanoTime();
		if (null != databaseConnection)
			m.databaseRoundTrip = databaseConnection.getLastCommitNS();
		m.inboundQueue = System.nanoTime();
		try {
			responseQueue.put(m);
		} catch (InterruptedException e) {
			Debug.println(Debug.ERROR, "Unable to queue up latencyMonitor return");
			e.printStackTrace();
		}
	}
}
