package uk.ac.cam.cl.juliet.slave.listening;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
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
	private Socket socket = null;
	private String ip;
	private int port;
	private ObjectInputStream input = null;
	private ObjectOutputStream output = null;
	private ArrayBlockingQueue<Container> responseQueue = new ArrayBlockingQueue<Container>(5100);
	private ArrayBlockingQueue<Container> receiveQueue = new ArrayBlockingQueue<Container>(5100);
	private LinkedList<XDPRequest> waitingForBatchQueries = new LinkedList<XDPRequest>();
	private ReentrantLock waitingForBatchQueriesLock = new ReentrantLock();
	private DatabaseConnection databaseConnection;
	private XDPProcessor xdp;
	private QueryProcessor query;
	
	private static int OUTPUT_RESET_LIMIT = 100000;
	private static long initialMs = 500;
	private long delayMs = initialMs;
	private static long cutOff = 10000;
	
	private Thread sendThread, receiveThread;
	
	private void interruptComms() {
		sendThread.interrupt();
		receiveThread.interrupt();
	}
	public synchronized boolean connect(String ip, int port) {
		Debug.println(Debug.INFO,"RUNNING CONNECT METHOD");
		if(delayMs <= cutOff) {
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			delayMs *= 2;
		} else {
			Debug.println(Debug.SHOWSTOP, "System restarting due to inability to reconnect");
			System.exit(0);
		}
		try {
			if(null != output) {
				output.close();
				output = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if(null != input) {
				input.close();
				input = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			this.socket = new Socket(ip,port);
			this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			output.flush();
			this.input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		delayMs = initialMs;
		return true;
	}

	/**
	 * Connects to the server and begins to read and process packets.
	 * 
	 * @param server
	 *            The IP address of the server to connect to.
	 * @param thePort
	 *            The port to connect to on the server.
	 * @param db
	 *            A connection to the database.
	 * @param xdpProcessor
	 *            The object which is used to process XDP packets.
	 * @param queryProcessor
	 *            The object which is used to process queries.
	 * @throws IOException
	 * @throws SQLException
	 */
	public void listen(String server, int thePort, DatabaseConnection db, XDPProcessor xdpProcessor, QueryProcessor queryProcessor) throws IOException, SQLException {
		this.ip = server;
		this.port = thePort;
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
				while (true) {
					readPacket();
				}
			}
		};		
		readThread.start();
		
		while(true) {
			while(!connect(ip,port)) continue;
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
							Debug.println(Debug.ERROR, "Unrecognised object type");
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
							return;
						} catch (IOException e) {
							interruptComms();
							Debug.println(Debug.ERROR, "An error occurred communicating with the server.");
							e.printStackTrace();
							// Just attempt to reconnect
							return;
						}
					}
				}
			};
			receiveThread.start();
	
			// Sends any waiting responses back to the server.
			Thread sendThread = new Thread() {
				public void run() {
					int packetCounter = 0;
					while (true) {
						try {
							Container response = responseQueue.take();
							if (response instanceof LatencyMonitor) {
								LatencyMonitor m = (LatencyMonitor) response;
								m.inboundDepart = System.nanoTime();
							}
							output.writeObject(response);
							output.flush();
							packetCounter++;
							if(packetCounter >= OUTPUT_RESET_LIMIT) {
								packetCounter = 0;
								output.reset();
								Debug.println(Debug.INFO, "reset ouputStream on Pi");
							}
							Debug.println("sent: size: " + responseQueue.size());
						} catch (IOException e) {
							e.printStackTrace();
							// Just attempt to reconnect
							interruptComms();
							return;
						} catch (InterruptedException e) {
							e.printStackTrace();
							return;
						}
					}
				}
			};
			sendThread.start();
			
			try  {
				sendThread.join();
				receiveThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Debug.println(Debug.ERROR, "WHY, I WAS INTERRUPTED WAITING FOR THEM TO BE INTERRUPTED?!!");
			}
		}
	}

	private void flushWaitingForBatchQueries() {
		Debug.println(Debug.INFO, "Starting flush: " + waitingForBatchQueries.size());
		XDPResponse response = null;
		XDPRequest r = null;
		while (waitingForBatchQueries.peek() != null) {
			r = waitingForBatchQueries.pop();
			response = new XDPResponse(r.getPacketId(), true);
			try {
				responseQueue.put(response);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Debug.println(Debug.INFO, "Finished flush: " + waitingForBatchQueries.size());
	}

	private void readPacket() {
		try {
			Container container = receiveQueue.take();

			Debug.println(100, "Got new object: " + container.toString());

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
				Debug.println(100, "Time taken for processing: " + diff + "ms");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processXDPRequest(XDPRequest container) {
		boolean result = this.xdp.decode(container);
		if (result == false) {
			// The decoding did not require the database - send the response straight back.
			XDPResponse response = new XDPResponse(container.getPacketId(), false);
			try {
				responseQueue.put(response);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			// The decoding required the database - wait until all changes have been committed.
			waitingForBatchQueriesLock.lock();
			waitingForBatchQueries.add(container);
			waitingForBatchQueriesLock.unlock();
			return;
		}
	}

	private void processQueryPacket(QueryPacket container) {
		try {
			responseQueue.put(query.runQuery(container));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	private void handleConfigurationPacket(ConfigurationPacket packet) {
		String ip = packet.getSetting("db.addr");
		if (ip != null) {
			try {
				this.databaseConnection.setConnection(DriverManager.getConnection("jdbc:mysql://" + ip + ":3306/juliet?rewriteBatchedStatements=true&useServerPrepStmts=false", "root", "rootword"));

			} catch (SQLException e) {
				Debug.println(Debug.ERROR, "An error occurred connecting to the database");
				e.printStackTrace();
				System.exit(1);
			}
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
