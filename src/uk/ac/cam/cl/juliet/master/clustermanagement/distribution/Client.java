package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.cam.cl.juliet.common.Container;
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
	private InetAddress address = null;
	private ClusterMaster parent = null;
	
	private int numberPooledThreads = 1;
	private static long queueFlushTime = 500; //in ms
	private static ScheduledExecutorService workers = null;
	
	private ScheduledFuture<?> cleaner = null;
	private ArrayBlockingQueue<Container> sendQueue = new ArrayBlockingQueue<Container>(20);
	
	private static AtomicInteger numberClients = new AtomicInteger(0);
	
	private static ContainerTimeComparator comparator = new ContainerTimeComparator();
	//Keep track of the objects in flight
	private ConcurrentHashMap<Long,InFlightContainer> hash = new ConcurrentHashMap<Long,InFlightContainer>();
	private PriorityBlockingQueue<InFlightContainer> jobqueue = new PriorityBlockingQueue<InFlightContainer>(16,comparator);
	
	private long totalPackets = 0;
	private int workCount = 0;
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
		workCount++;
		jobqueue.add(container);
		System.out.println("Added to job queue: " + jobqueue.size());		
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
		System.out.println("About to get backing: " + l);
		if(null != cont) {
			cont.setReplyRecieved();
			hash.remove(l);
			// Pretty sure this should be here? - Scott
			jobqueue.remove(cont);
			System.out.println("Removed from job queue: " + l);
			workCount--;
		} else {
			System.out.println("Null InFlightContainer recieved from hash");
		}
		return cont;
	}
	/**
	 * This method closes the connection to the client and removes it from the associated
	 * ClusterMaster so that no more work is allocated. It does not salvage the 
	 * jobs waiting on returns.
	 */
	public void closeClient() {
		parent.removeClient(this);
		//Close the streams
		if(0 == numberClients.decrementAndGet()) {
			workers.shutdownNow();
			workers = null;
		}
		cleaner.cancel(false); //Try to stop the regular operation flushing my queue
		try {
			out.close();
			in.close(); //Should also have the effect of closing the threads that read and write on them
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Client(Socket s,ClusterMaster parent) {
		this.parent = parent;
		if( 0 == numberClients.getAndIncrement() ) {
			workers = Executors.newScheduledThreadPool(numberPooledThreads);
		}
		//Schedule queueflush for me
		cleaner = workers.scheduleAtFixedRate(new ClientCleanup(this), 0, queueFlushTime, TimeUnit.MILLISECONDS);
		
		address = s.getInetAddress();
		try {
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());
			out.writeObject(parent.getConfiguration());
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
						System.out.println("Boom");
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(null == recieve) continue;
					if(recieve instanceof Container) {
						//fantastic!
						//Count the packets back in
						Container container = (Container)recieve;
						System.out.println("recieved ack for: " + container.getPacketId());
						
						InFlightContainer record = checkbackContainer(container);
						if(record != null)
							record.executeCallback(container);
						//Packet dealt with
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
				while(true) {
					try {
						Container c = sendQueue.take();
						InFlightContainer container = new InFlightContainer(c);
						checkoutContainer(container);
						out.writeObject(c);
						totalPackets++; //TODO this means it won't count objects in the queue
						System.out.println("Written obj to client");
					} catch ( InterruptedException e){
						e.printStackTrace();
						return;
					} catch ( IOException e) {
						e.printStackTrace();
						closeClient();
						return;
					}
					if(this.isInterrupted()) {
						System.out.println("BAD");
						return; //In case it's not thrown whilst waiting?
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
		return workCount;
	}
	/**
	 * @return The number of Containers that have been sent out to this Client,
	 * since the object was created - the Client itself is not queried.
	 */
	public long getTotalWork() {
		//The total amount of work done since the beginning
		return totalPackets;
	}
	
	private long send(Container c,long uid) {
		c.setPacketId(uid);
		try {
			sendQueue.put(c);
			System.out.println("Added to send queue: " + sendQueue.size());
		} catch (InterruptedException e) {
			return -1;
		}
		return uid;
	}
	/**
	 * This method sends the specified container to the Pi, logging it as it does so.
	 * @param c
	 * @return The unique id of the packet being sent
	 */
	public long send (Container c) {
		long uid = parent.getNextId();
		return send(c,uid);
	}
	
	public long broadcast(Container c) {
		return send(c,c.getPacketId());
	}
	//TODO mark a broadcast one as a message that it doesn't cascade on failure
	
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
		while( null != (ifc = jobqueue.peek())) {
			if(ifc.hasReplyRecieved()) {
				ifc = jobqueue.poll();
				if(null == ifc) break;
				if(!ifc.hasReplyRecieved()) {
					jobqueue.add(ifc);
					break;
				}
			} else if( ifc.getDueTime() <= time ) {
				//Remove and flush the rest of the queue
				closeClient();
				while(null != (ifc = jobqueue.poll())) {
					if(!ifc.hasReplyRecieved()) {
						//Reply hasn't been received so need to send again on a different node
						System.out.println("Resending packet");
						try {
							parent.sendPacket(ifc.getContainer(),ifc.getCallback());
						} catch (NoClusterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				break;
			}
		}
	}
}