package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.LatencyMonitor;

final class ContainerTimeComparator implements Comparator<InFlightContainer> {
	public int compare(InFlightContainer o1, InFlightContainer o2) {
		return (int) (o1.getDueTime() - o2.getDueTime());
	}
}

class ClientCleanup implements Runnable {
	Client client;

	public void run() {
		client.tryFlushQueue();
	}

	public ClientCleanup(Client c) {
		client = c;
	}
}


/**
 * In charge of sending and receiving packets foe a specific client 
 * 
 * @author Joseph
 */
public class Client implements Comparable<Client>{
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Socket s = null;
	private InetAddress address = null;
	private ClusterMaster parent = null;

	private int numberPooledThreads = 1;
	private static long queueFlushTime = 500; // in ms
	private static ScheduledExecutorService workers = null;

	private ScheduledFuture<?> cleaner = null;
	private ArrayBlockingQueue<InFlightContainer> sendQueue = new ArrayBlockingQueue<InFlightContainer>(200);

	private static AtomicInteger numberClients = new AtomicInteger(0);
	private AtomicInteger workCount = new AtomicInteger(0);

	private static ContainerTimeComparator comparator = new ContainerTimeComparator();
	
	private ConcurrentHashMap<Long, InFlightContainer> hash = new ConcurrentHashMap<Long, InFlightContainer>();
	private PriorityBlockingQueue<InFlightContainer> jobqueue = new PriorityBlockingQueue<InFlightContainer>(16, comparator);

	private long totalPackets = 0;
	public int packetsSentThisSecond = 0;

	private static final int OUTPUT_RESET_LIMIT = 50000;

	private Semaphore sem = new Semaphore(5000); // Allows 5000 at one time

	private boolean amClosing = false;

	/**
	 * Get the IP address of the Client that this Client object is connected to.
	 * 
	 * @return The InetAAddress of the client
	 */
	public InetAddress getClientIP() {
		return address;
	}

	/**
	 * This increments the work count and logs the container in the relevant
	 * lists.
	 * 
	 * @param container
	 */
	private void checkoutContainer(InFlightContainer container) {
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		jobqueue.add(container);
		hash.put(container.getPacketId(), container);
	}

	/**
	 * This marks the container as back and removes form some lists, as well as
	 * decrementing the number of active packets on this cluster.
	 * 
	 * @param container
	 * @return The IFC related to this interaction.
	 */
	private InFlightContainer checkbackContainer(Container container) {
		Long l = container.getPacketId();
		InFlightContainer cont = hash.get(l);
		Debug.println("Received ack for packet ID: " + l);
		if (null != cont) {
			sem.release();
			jobqueue.remove(cont);
			hash.remove(l);
			workCount.decrementAndGet();
		} else {
			Debug.println(Debug.ERROR, "Null InFlightContainer recieved from hash");
		}
		return cont;
	}

	/**
	 * This method closes the connection to the client and removes it from the
	 * associated ClusterMaster so that no more work is allocated. It does not
	 * salvage the jobs waiting on returns.
	 */
	public void closeClient() {
		if (amClosing) {
			return;
		}
		amClosing = true;
		
		Debug.println(Debug.INFO, "closeClient was called on client" + address.toString());

		parent.removeClient(this);
		cleaner.cancel(false);
		
		if (0 == numberClients.decrementAndGet()) {
			workers.shutdown();
			workers = null;
		}

		try {
			out.close();
			in.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fullyFlushQueues();
		amClosing = false;
	}

	public Client(Socket s, ClusterMaster parent) {
		this.parent = parent;
		this.s = s;
		
		if (0 == numberClients.getAndIncrement()) {
			workers = Executors.newScheduledThreadPool(numberPooledThreads);
		}
		
		while (null == workers)
			continue; // Someone else is creating it.

		this.cleaner = workers.scheduleAtFixedRate(new ClientCleanup(this), 0, queueFlushTime, TimeUnit.MILLISECONDS);
		this.address = s.getInetAddress();
		
		try {
			this.out = new ObjectOutputStream(s.getOutputStream());
			this.in = new ObjectInputStream(s.getInputStream());
			out.writeObject(parent.getConfiguration());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Run myself to start listening for objects being sent my way
		Thread listener = new Thread() {
			@Override
			public void run() {
				while (true) {
					Object recieve = null;
					try {
						recieve = in.readObject();
						Debug.println("Received an object from client...");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (Exception e) {
						Debug.println(Debug.SHOWSTOP, e.getMessage());
						e.printStackTrace();
						closeClient();
						return;
					}
					if (null == recieve)
						continue;
					if (recieve instanceof Container) {
						// Count the packet back in
						Container container = (Container) recieve;
						if (container instanceof LatencyMonitor) {
							LatencyMonitor lm = (LatencyMonitor) container;
							lm.inboundArrive = System.nanoTime();
							// Identify which Pi this came from
							lm.addr = getClientIP().toString();
						}
						
						InFlightContainer record = checkbackContainer(container);
						if (record != null)
							record.executeCallback(container);
					}
				}
			}
		};
		listener.start();

		Thread send = new Thread() {
			@Override
			public void run() {
				// This method sends the packets to the client
				long then = System.nanoTime();
				int packetCounter = 0;
				while (true) {
					try {
						InFlightContainer container = sendQueue.take();
						checkoutContainer(container);
						Container c = container.getContainer();
						if (c instanceof LatencyMonitor) {
							LatencyMonitor m = (LatencyMonitor) c;
							m.outboundDepart = System.nanoTime();
						}
						out.writeObject(c);
						out.flush();
						packetCounter++;
						if (packetCounter >= OUTPUT_RESET_LIMIT) {
							packetCounter = 0;
							out.reset();
							Debug.println(Debug.ERROR, "reset outputStream on client");
						}
						totalPackets++;
						packetsSentThisSecond++;
						if (Math.abs(System.nanoTime() - then) > 1000000000) {
							Debug.println(100, "Packets sent this second: " + packetsSentThisSecond + ", to client: " + getClientIP().toString());
							then = System.nanoTime();
							packetsSentThisSecond = 0;
						}
						Debug.println("Written packet ID: " + container.getPacketId());
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (Exception e) {
						Debug.println(Debug.SHOWSTOP, e.getMessage());
						e.printStackTrace();
						closeClient();
						return;
					}
				}
			}
		};
		send.start();
	}

	/**
	 * 
	 * @return The current number of Containers out on this Client
	 */
	public int getCurrentWork() {
		return workCount.get();
	}

	/**
	 * @return The number of Containers that have been sent out to this Client,
	 *         since the object was created - the Client itself is not queried.
	 */
	public long getTotalWork() {
		return totalPackets;
	}

	private long send(Container c, Callback cb, long uid, boolean bcast) {
		c.setPacketId(uid);
		InFlightContainer ifc = new InFlightContainer(c, cb);
		ifc.setBroadcast(bcast);
		try {
			Debug.println("About to add to send queue");
			if (c instanceof LatencyMonitor) {
				LatencyMonitor m = (LatencyMonitor) c;
				m.outboundQueue = System.nanoTime();
			}
			if (!sendQueue.offer(ifc, 200, TimeUnit.MILLISECONDS))
				return -1;
			workCount.incrementAndGet();
			Debug.println("Added to send queue");
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return uid;
	}

	/**
	 * This method sends the specified container to the Pi, logging it as it
	 * does so.
	 * 
	 * @param c
	 *            The container to send
	 * @return The unique id of the packet being sent
	 */
	public long send(Container c) {
		long uid = parent.getNextId();
		return send(c, null, uid, false);
	}

	/**
	 * This method sends the specified container to the Pi, logging it as it
	 * does so.
	 * 
	 * @param c
	 *            The Container to send
	 * @param cb
	 *            The callback to be run
	 * @return The unique id of the packet being sent
	 */
	public long send(Container c, Callback cb) {
		long uid = parent.getNextId();
		return send(c, cb, uid, false);
	}

	/**
	 * Send the packet to this Client ensuring that it knows that on failure
	 * resend should not be attempted
	 * 
	 * @param c
	 *            The container to broadcast
	 * @return The id of the broadcast packet
	 */
	public long broadcast(Container c) {
		return send(c, null, c.getPacketId(), true);
	}

	/**
	 * Send the packet to this Client ensuring that it knows that on failure
	 * resend should not be attempted
	 * 
	 * @param c
	 *            The container to broadcast
	 * @param cb
	 *            The callback to be run when a response to the container is
	 *            received from the client.
	 * @return The id of the broadcast packet
	 */
	public long broadcast(Container c, Callback cb) {
		return send(c, cb, c.getPacketId(), true);
	}

	private void fullyFlushQueues() {
		InFlightContainer ifc;
		while (null != (ifc = jobqueue.poll())) {
			if (!ifc.getBroadcast()) {
				Debug.println(Debug.INFO, "Resending packet: " + ifc.getPacketId());
				try {
					parent.sendPacket(ifc.getContainer(), ifc.getCallback());
				} catch (NoClusterException e) {
					e.printStackTrace();
				}
			}
		}
		while (null != (ifc = sendQueue.poll())) {
			if (!ifc.getBroadcast()) {
				Debug.println(Debug.INFO, "Resending packet from waiting: " + ifc.getPacketId());
				try {
					parent.sendPacket(ifc.getContainer(), ifc.getCallback());
				} catch (NoClusterException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This method checks the front of the priority queue ( the timeout that'll
	 * expire soonest, if it finds the timeout has passed then the client is
	 * closed, removed from the master and all jobs that are still out for
	 * processing and transferred back to the ClusterMaster to attempt sending
	 * to another node. It's unlikely you'll ever need to call this method as it
	 * is automatically run at specified intervals - see source code. Also this
	 * method is probably NOT THREAD SAFE As it's automatically run you never
	 * know when it might be running so don't call it!
	 */

	public void tryFlushQueue() {
		long time = System.nanoTime();
		InFlightContainer ifc;
		if (null != (ifc = jobqueue.peek())) {
			if (ifc.getDueTime() <= time) {
				// Remove and flush the rest of the queue
				Debug.println(Debug.ERROR, "Timeout waiting for response from client " + address + ", packet: " + ifc.getContainer().getPacketId() + ",: " + ifc.getContainer().toString());
				closeClient();
			}
		}
	}

	@Override
	public int compareTo(Client o) {
		// TODO Auto-generated method stub
		return this.getCurrentWork() - o.getCurrentWork();
	}
}
