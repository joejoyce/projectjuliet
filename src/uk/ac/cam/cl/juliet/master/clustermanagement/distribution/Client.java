package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.LatencyMonitor;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;

final class ContainerTimeComparator implements Comparator<InFlightContainer> {
	public int compare(InFlightContainer o1, InFlightContainer o2){
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

public class Client {
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Socket s;
	private InetAddress address = null;
	private ClusterMaster parent = null;
	
	private int numberPooledThreads = 1;
	private static long queueFlushTime = 500; //in ms
	private static ScheduledExecutorService workers = null;
	
	private ScheduledFuture<?> cleaner = null;
	private ArrayBlockingQueue<InFlightContainer> sendQueue = new ArrayBlockingQueue<InFlightContainer>(200);
	
	private static AtomicInteger numberClients = new AtomicInteger(0);
	private AtomicInteger workCount = new AtomicInteger(0);
	
	private static ContainerTimeComparator comparator = new ContainerTimeComparator();
	//Keep track of the objects in flight
	private ConcurrentHashMap<Long,InFlightContainer> hash = new ConcurrentHashMap<Long,InFlightContainer>();
	private PriorityBlockingQueue<InFlightContainer> jobqueue = new PriorityBlockingQueue<InFlightContainer>(16,comparator);
	
	private long totalPackets = 0;
	public int packetsSentThisSecond = 0;
	
	/**
	 *  Get the IP address of the Client that this Client object is connected to.
	 * @return The InetAAddress of the client
	 */
	public InetAddress getClientIP() {
		return address;
	}
	/**
	 * This increments the work count and logs the container in the relevant lists.
	 * @param container
	 */
	private void checkoutContainer(InFlightContainer container) {
		workCount.incrementAndGet();
		jobqueue.add(container);
		hash.put(container.getPacketId(), container);
	}
	
	/**
	 * This marks the container as back and removes form some lists,
	 * as well as decrementing the number of active packets on this cluster.
	 * @param container
	 * @return The IFC related to this interaction.
	 */
	private InFlightContainer checkbackContainer(Container container) {
		Long l = container.getPacketId();
		InFlightContainer cont = hash.get(l);
		Debug.println("Received ack for packet ID: " + l);
		if(null != cont) {
			jobqueue.remove(cont);
			hash.remove(l);
			workCount.decrementAndGet();
		} else {
			Debug.println(Debug.ERROR,"Null InFlightContainer recieved from hash");
		}
		return cont;
	}
	/**
	 * This method closes the connection to the client and removes it from the associated
	 * ClusterMaster so that no more work is allocated. It does not salvage the 
	 * jobs waiting on returns.
	 */
	public void closeClient() {
		// Must be here, don't move plox
		sendQueue = null;
		
		parent.removeClient(this);
		//Close the streams
		if(0 == numberClients.decrementAndGet()) {
			// Commented this out for now - scott
			//workers.shutdownNow();
			//workers = null;
		}						
		cleaner.cancel(false); //Try to stop the regular operation flushing my queue, waiting until finished
		try {
			Debug.println(100 ,"---------------Close Client has been called----------------");
			out.close();
			in.close(); //Should also have the effect of closing the threads that read and write on them
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Client(Socket s, ClusterMaster parent) {
		this.parent = parent;
		this.s = s;
		if(0 == numberClients.getAndIncrement() ) {
			workers = Executors.newScheduledThreadPool(numberPooledThreads);
		}
		//Schedule queueflush for me
		cleaner = workers.scheduleAtFixedRate(new ClientCleanup(this), 0, queueFlushTime, TimeUnit.MILLISECONDS);
		
		address = s.getInetAddress();
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			out.flush();
			in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
			out.writeObject(parent.getConfiguration());
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Run myself to start listening for objects being sent my way!
		Thread listener = new Thread() {
			@Override
			public void run() {
				//This method needs to use the
				while(true) {
					Object recieve = null;
					try {
						recieve = in.readObject();
						Debug.println("Received an object from client...");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (Exception e) {
						Debug.println(Debug.SHOWSTOP,e.getMessage());
						e.printStackTrace();
						closeClient();
						return;
					}
					if(null == recieve) continue;
					if(recieve instanceof Container) {
						//fantastic!
						//Count the packets back in
						Container container = (Container)recieve;
						if(container instanceof LatencyMonitor) {
							LatencyMonitor lm = (LatencyMonitor)container;
							lm.inboundArrive = System.nanoTime();
							lm.addr = getClientIP().toString();
							//Identify which Pi this came from
						}
						InFlightContainer record = checkbackContainer(container);
						if(record != null)
							record.executeCallback(container);
					}
					//Otherwise ignore for the moment
				}
			}
		};
		listener.start();
		
		Thread send = new Thread() {
			@Override
			public void run() {
				//This method sends the packets to the client
				long then = System.nanoTime();
				while(true) {
					try {
						InFlightContainer container = sendQueue.take();
						checkoutContainer(container);
						Container c = container.getContainer();
						if(c instanceof LatencyMonitor) {
							LatencyMonitor m = (LatencyMonitor)c;
							m.outboundDepart = System.nanoTime();
						}
						out.writeObject(c);
						out.flush();												
						totalPackets++; //TODO this means it won't count objects in the queue
						packetsSentThisSecond ++;
						if(Math.abs(System.nanoTime() - then) > 1000000000) {
							Debug.println(100, "Packets sent this second: " + packetsSentThisSecond);
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
		//This is the amount of work which has not been accounted for
		return workCount.get();
	}
	/**
	 * @return The number of Containers that have been sent out to this Client,
	 * since the object was created - the Client itself is not queried.
	 */
	public long getTotalWork() {
		//The total amount of work done since the beginning
		return totalPackets;
	}
	
	private long send(Container c, Callback cb, long uid, boolean bcast) {
		c.setPacketId(uid);
		InFlightContainer ifc = new InFlightContainer(c,cb);
		ifc.setBroadcast(bcast);
		try {
			Debug.println(100, "About to add to send queue: " + sendQueue.size());
			if(c instanceof LatencyMonitor) {
				LatencyMonitor m = (LatencyMonitor)c;
				m.outboundQueue = System.nanoTime();
			}
			while(!sendQueue.offer(ifc)) {}
			Debug.println(100, "Added to send queue: " + sendQueue.size());
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		return uid;
	}
	/**
	 * This method sends the specified container to the Pi, logging it as it does so.
	 * @param c The container to send
	 * @return The unique id of the packet being sent
	 */
	public long send(Container c) {
		long uid = parent.getNextId();
		return send(c,null,uid,false);
	}
	/**
	 * This method sends the specified container to the Pi, logging it as it does so.
	 * @param c The Container to send
	 * @param cb The callback to be run
	 * @return The unique id of the packet being sent
	 */	
	public long send(Container c,Callback cb) {
		long uid = parent.getNextId();
		return send(c,cb,uid,false);
	}
	
	/**
	 * Send the packet to this Client ensuring that it knows that on failure resend should not be attempted
	 * @param c The container to broadcast
	 * @return The id of the broadcast packet
	 */
	public long broadcast(Container c) {
		return send(c, null, c.getPacketId(), true);
	}
	
	public long broadcast(Container c, Callback cb) {
		return send(c, cb, c.getPacketId(), true);
	}
	
	
	/**
	 * This method checks the front of the priority queue ( the timeout that'll expire 
	 * soonest, if it finds the timeout has passed then the client is closed, removed from
	 * the master and all jobs that are still out for processing and transferred back to the
	 * ClusterMaster to attempt sending to another node.
	 * It's unlikely you'll ever need to call this method as it is automatically run at
	 * specified intervals - see source code. Also this method is probably NOT THREAD SAFE
	 * As it's automatically run you never know when it might be running so don't call it!
	 */
	
	public void tryFlushQueue() {
		long time = System.nanoTime();
		InFlightContainer ifc;
		if( null != (ifc = jobqueue.peek())) {
			if( ifc.getDueTime() <= time ) {
				//Remove and flush the rest of the queue
				Debug.println(Debug.ERROR,"Timeout waiting for response from client " + address);
				closeClient();
				while(null != (ifc = jobqueue.poll())) {
					if(!ifc.getBroadcast()) {
						//Reply hasn't been received and not broadcast so resend
						Debug.println(Debug.INFO,"Resending packet: " + ifc.getPacketId());
						try {
							parent.sendPacket(ifc.getContainer(),ifc.getCallback());
						} catch (NoClusterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}