package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

//import org.dhcp4java.DHCPCoreServer;
//import org.dhcp4java.DHCPServerInitException;
//import org.dhcp4java.DHCPServlet;


import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;


final class ClientLoadComparator implements Comparator<Client> {
	public int compare(Client o1, Client o2){
		return (int) (o1.getCurrentWork() - o2.getCurrentWork() );
	}
}


class RepeatedSend implements Runnable{
	private ClusterMaster cm;
	private Container c;
	private Callback cb;
	public RepeatedSend(ClusterMaster cm, Container c, Callback cb) {
		this.c = c;
		this.cm = cm;
		this.cb = cb;
	}

	@Override
	public void run() {
		try {
			cm.sendPacket(c,cb);
		} catch (NoClusterException e) {
			// TODO Auto-generated catch block
			Debug.println(Debug.ERROR,"Problems sending a packet scheduled for repeated Send");
			e.printStackTrace();
		}
	}
}
/**
 * This class handles all data being sent to the cluster - when it recieves a packet it allocates
 * it to the Client with least work currently.
 * It is also responsible for providing both settings information to the raspberry Pi and DHCP services.
 * This class can also be queried for performance statistics for the cluster.
 *  
 * @author joseph
 *
 */

public class ClusterMasterUnit implements ClusterMaster  {
	final static long queueFlushInteval = 500;

	private ConfigurationPacket cp = new ConfigurationPacket();
	
	private ServerSocket socket = null;
	private AtomicLong nextId = new AtomicLong(0);
	public long currentSystemTime;
	
	private static ClientLoadComparator clc = new ClientLoadComparator();
	private PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<Client>(16,clc);
	private CopyOnWriteArrayList<Client> allClients = new CopyOnWriteArrayList<Client>();
	
	private ScheduledExecutorService workers = null;
	
	public ClusterMasterUnit(String filename) {
		StringReader r = new StringReader(filename);
		BufferedReader bf = new BufferedReader(r);
		try {
			bf.readLine();
			String line = null;
			while(null != (line = bf.readLine())) {
				String arr[] = line.split(" ");
				if(arr.length > 1)
					cp.setSetting(arr[0],arr[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void addClient(Socket skt) {
		Client c = new Client(skt,this);
		clientQueue.add(c);
		allClients.add(c);
	}
	
	@Override
	public void start(int port) throws IOException {
		if(workers == null)
			workers = Executors.newScheduledThreadPool(1);
		if(null != socket)
			socket.close();
		socket = new ServerSocket(port);
		
		Thread t = new Thread () {
			@Override
			public void run() {
				while(true) {
					try {
						Socket connection = socket.accept();
						Debug.println("About to add a new client!");						
						addClient(connection);
						Debug.println(100, "Added a new client!\nTotal Clients: " + allClients.size());					
					} catch (IOException e) {
						System.out.println("There was an error establishing a connection and spawning a client");
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
	}
	
	@Override
	public void stop() {
		if(null != socket) {
			if(null != workers) {
				workers.shutdownNow();
				workers = null;
			}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} //This should have the effect of cleaning up
		}
		socket = null;
	}
	
	@Override
	public long sendPacket(Container msg) throws NoClusterException {
		//Send the message - the process doesn't care about the reply
		return sendPacket(msg,(Callback)null);
	}
	
	@Override
	public long sendPacket(Container msg, Callback cb) throws NoClusterException {
		Client c = clientQueue.poll();
		while (c == null) {
			/*System.out.println("Was null: " + clientQueue.size());*/
			try {
				c = clientQueue.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
		}

		long l = c.send(msg,cb);
		clientQueue.put(c);
		currentSystemTime = msg.getTimeStampS();
		return l;
	}
	
	@Override
	public void removeClient(Client ob) {
		clientQueue.remove(ob);
		allClients.remove(ob);
	}
	
	@Override
	public void closeAndRemove(Client ob) {
		ob.closeClient();
		removeClient(ob);
	}
	
	@Override
	public long getNextId() {
		return nextId.incrementAndGet();
	}
	
	@Override
	public String getSetting( String key ) {
		return cp.getSetting(key);
	}
	
	@Override
	public void setSetting ( String key, String value) {
		cp.setSetting(key,value);
		//Push out to all clients
		broadcast(cp);
	}
	
	@Override
	public ConfigurationPacket getConfiguration() {
		return cp;
	}
	
	@Override
	public Client[] listClients() {
		Client[] arr = allClients.toArray(new Client[0]);
		return arr;
	}
	
	public long getTime() {
		return currentSystemTime;
	}
	
	public int getPacketThroughput() {
		int total = 0;
		for(Client c : allClients) {
			total += c.packetsSentThisSecond;
		}		
		return total;
	}
	
	@Override
	public void broadcast(Container c) {
		c.setPacketId(getNextId());
		Iterator<Client> iter = clientQueue.iterator();
		while(iter.hasNext())
			iter.next().broadcast(c);
	}
	
	
	@Override
	public void broadcast(Container c, Callback cb) {
		c.setPacketId(getNextId());
		Iterator<Client> iter = clientQueue.iterator();
		while(iter.hasNext())
			iter.next().broadcast(c,cb);	
	}

	@Override
	public int getClientCount() {
		return allClients.size();
	}
	
	/**
	 * Repeatedly send the packet to a client, getting a callback each time.
	 * BEWARE if this fails it will retry after five seconds so this could easily fill the
	 * system!!
	 * @param c The container to send
	 * @param cb The callback to be run
	 * @param time In milliseconds between them - this is the time between attempted send
	 * @return A sechduledFuture - this can be used to cancel it etc...
	 */
	public ScheduledFuture<?> repeatedSend(Container c, Callback cb, long time) {
		RepeatedSend rs = new RepeatedSend(this,c,cb);
		return workers.scheduleAtFixedRate(rs, 0, time, TimeUnit.MILLISECONDS);
	}
	
}