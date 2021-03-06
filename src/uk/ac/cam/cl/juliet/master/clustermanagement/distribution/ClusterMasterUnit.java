package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.Heartbeat;
import uk.ac.cam.cl.juliet.master.ShutdownSettingsSaver;


final class ClientLoadComparator implements Comparator<Client> {
	public int compare(Client o1, Client o2){
		return (int) (o1.getCurrentWork() - o2.getCurrentWork() );
	}
}
final class ClientNameComparator implements Comparator<Client> {
	public int compare(Client o1, Client o2){
		String a = o1.getClientIP().toString(), b = o2.getClientIP().toString();
		//Debug.println(Debug.SHOWSTOP,a+" ::: " +b);
		a = a.substring(a.lastIndexOf(".") + 1);
		b = b.substring(b.lastIndexOf(".") + 1);
		//Debug.println(Debug.SHOWSTOP,a+" ::: " +b);
		int aint = Integer.parseInt(a), bint = Integer.parseInt(b);
		return aint - bint;
	}
}

class RepeatedSend implements Runnable {
	private ClusterMaster cm;
	private Container c;
	private Callback cb;
	private boolean repeat = false;
	public RepeatedSend(ClusterMaster cm, Container c, Callback cb) {
		this.c = c;
		this.cm = cm;
		this.cb = cb;
	}
	public RepeatedSend(ClusterMaster cm, Container c, Callback cb, boolean repeat ) {
		this.c = c;
		this.cm = cm;
		this.cb = cb;
		this.repeat = repeat;
	}

	@Override
	public void run() {
		Container ncont = null;
		try {
			ncont = (Container) c.clone();
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if(repeat)
				cm.broadcast(ncont,cb);
			else
				cm.sendPacket(ncont,cb);
		} catch (NoClusterException e) {
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
	public volatile long currentSystemTime;
	
	private static ClientLoadComparator clc = new ClientLoadComparator();
	private static ClientNameComparator cnc = new ClientNameComparator();
	private SuperFancyConcurrentPriorityQueue<Client> clientQueue = new SuperFancyConcurrentPriorityQueue<Client>(clc,cnc);
	private ScheduledExecutorService workers = null;
	
	public ClusterMasterUnit(Map<String, String> settings, ShutdownSettingsSaver saver) {
		for(Entry<String, String> entry : settings.entrySet()) {
			cp.setSetting(entry.getKey(), entry.getValue());
		}
		saver.setConfigurationPacket(cp);
	}
	
	@Override
	public void addClient(Socket skt) {
		Client c = new Client(skt,this);
		try {
			clientQueue.push(c);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start(int port) throws IOException {
		if(workers == null)
			workers = Executors.newScheduledThreadPool(1);
		if(null != socket)
			socket.close();
		socket = new ServerSocket(port);
		
		repeatedBroadcast(new Heartbeat(),null,1000);
		
		Thread t = new Thread () {
			@Override
			public void run() {
				while(true) {
					try {
						Socket connection = socket.accept();
						Debug.println("About to add a new client!");						
						addClient(connection);
						Debug.println(100, "Added a new client!\nTotal Clients: " + clientQueue.size());					
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
	public long sendPacket(Container msg, Callback cb) throws NoClusterException {
		Client c = clientQueue.peek();
		if(null == c)
			throw new NoClusterException("The Pis have all gone :'(" + clientQueue.size());
		long l = 0;
		while(0 > (l = c.send(msg,cb))) {
			c = clientQueue.peek();
			if(null == c)
				throw new NoClusterException("The Pis have all gone :'(: " + clientQueue.size());
		}
		currentSystemTime = msg.getTimeStampS();
		return l;
	}
	@Override
	public int broadcast(Container c, Callback cb) {
		c.setPacketId(getNextId());
		Iterator<Client> iter = clientQueue.iterator();
		int i = 0;
		for(;iter.hasNext();i++)
			iter.next().broadcast(c,cb);
		//clientQueue.releaseIterator();
		return i;
	}
	
	@Override
	public int broadcast(Container c) {
		return broadcast(c,(Callback)null);
	}
	@Override
	public long sendPacket(Container msg) throws NoClusterException {
		//Send the message - the process doesn't care about the reply
		return sendPacket(msg,(Callback)null);
	}
	
	@Override
	public void removeClient(Client ob) {
		clientQueue.remove(ob);
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
		// Push out to all clients
		broadcast(cp);
	}
	
	@Override
	public ConfigurationPacket getConfiguration() {
		return cp;
	}
	
	@Override
	public Client[] listClients() {
		Client arr[] = clientQueue.toArray(new Client[0]);
		return arr;
	}
	
	public long getTime() {
		return currentSystemTime;
	}
	
	public int getPacketThroughput() {
		int total = 0;
		Iterator<Client> i = clientQueue.iterator();
		while(i.hasNext())
			total += i.next().packetThroughput;
		//clientQueue.releaseIterator();
		return total;
	}
	
@Override
	public int getClientCount() {
		return clientQueue.size();
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
	public ScheduledFuture<?> repeatedSend(Container c, Callback cb, long timeMs) {
		c.setPacketId(this.getNextId());
		RepeatedSend rs = new RepeatedSend(this,c,cb);
		return workers.scheduleAtFixedRate(rs, 0, timeMs, TimeUnit.MILLISECONDS);
	}
	
	public ScheduledFuture<?> repeatedBroadcast(Container c, Callback cb, long timeMs) {
		RepeatedSend rs = new RepeatedSend(this,c,cb,true);
		return workers.scheduleAtFixedRate(rs,0,timeMs,TimeUnit.MILLISECONDS);
	}
	
}
