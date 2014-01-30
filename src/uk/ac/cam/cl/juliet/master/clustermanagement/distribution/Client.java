package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;


final class ContainerTimeComparator implements Comparator<InFlightContainer> {
	public int compare(InFlightContainer o1, InFlightContainer o2){
		return (int) (o1.getDueTime() - o2.getDueTime() );
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

public class Client implements Runnable{
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private InetAddress address = null;
	private ClusterMaster parent = null;
	
	private int numberPooledThreads = 10;
	private static long queueFlushTime = 500; //in ms
	private static ScheduledExecutorService workers = null;
	
	private Future cleaner = null;
	
	private static int numberClients = 0;
	
	private static ContainerTimeComparator comparator = new ContainerTimeComparator();
	//Keep track of the objects in flight
	private HashMap<Long,InFlightContainer> hash = new HashMap<Long,InFlightContainer>();
	private PriorityQueue<InFlightContainer> jobqueue = new PriorityQueue<InFlightContainer>(16,comparator);
	
	private long uniqueId = 0;
	private int workCount = 0;
	
	private void checkoutContainer(InFlightContainer container) {
		workCount++;
		jobqueue.add(container);
		hash.put(container.getPacketId(), container);
	}
	
	private InFlightContainer checkbackContainer(Container container) {
		Long l = container.getPacketId();
		InFlightContainer cont = hash.get(l);
		if(null != cont) {
			cont.setReplyRecieved();
			hash.remove(l);
			workCount--;
		}
		return cont;
	}
	
	private void closeClient() {
		parent.removeClient(this);
		//Close the streams
		if(0 == --numberClients) {
			workers.shutdownNow();
			workers = null;
		}
		cleaner.cancel(false); //Try to stop the regular operation flushing my queue
		try {
			out.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Client(Socket s,ClusterMaster parent) {
		this.parent = parent;
		if( 0 == numberClients++ ) {
			workers = Executors.newScheduledThreadPool(numberPooledThreads);
		}
		//Schedule queueflush for me
		cleaner = workers.scheduleAtFixedRate(new ClientCleanup(this), 0, queueFlushTime, TimeUnit.MILLISECONDS);
		
		address = s.getInetAddress();
		try {
			out = new ObjectOutputStream(s.getOutputStream());
			in = new ObjectInputStream(s.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//TODO send configuration packet here
		
		//Run myself to start listening for objects being sent my way!
		new Thread(this);
	}
	public int getCurrentWork() {
		//This is the amount of work which has not been accounted for
		return workCount;
	}
	public long getTotalWork() {
		//The total amount of work done since the beginning
		return uniqueId;
	}
	
	public void send (Container c) {
		c.setPacketId(uniqueId++);
		InFlightContainer container = new InFlightContainer(c);
		checkoutContainer(container);
		try {
			out.writeObject(c);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//Force close this raspberry pi
			closeClient();
			
		}
	}

	@Override
	public void run() {
		//This method needs to use the
		while(true) {
			Object recieve = null;
			try {
				recieve = in.readObject();
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
				InFlightContainer record = checkbackContainer(container);
				if(record != null)
					record.executeCallback(container);
				//Packet dealt with
			}
			//Otherwise ignore for the moment
		}
	}
	
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
						//Reply hasn't been recieved so need to send again on a different node
						parent.sendPacket(ifc.getContainer(),ifc.getCallback());
					}
				}
				break;
			}
		}

	}
	
}
