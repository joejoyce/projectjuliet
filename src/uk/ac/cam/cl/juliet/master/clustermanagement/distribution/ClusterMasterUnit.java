package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

//import org.dhcp4java.DHCPCoreServer;
//import org.dhcp4java.DHCPServerInitException;
//import org.dhcp4java.DHCPServlet;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;


final class ClientLoadComparator implements Comparator<Client> {
	public int compare(Client o1, Client o2){
		return (int) (o1.getCurrentWork() - o2.getCurrentWork() );
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
	
	private static ClientLoadComparator clc = new ClientLoadComparator();
	private PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<Client>(16,clc);
	//TODO need to sort out the priorityblocking queue so that it can be efficiently reordered on one update
	//Make my own queue that also has fast random access so can be used for both ?
	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void addClient(Socket skt) {
		Client c = new Client(skt,this);
		clientQueue.add(c); 
		//TODO sort this out
	}
	
	@Override
	public void start(int port) throws IOException {
		/*if(null == dhcpServer) {
		    try {
		    	dhcpServer = DHCPCoreServer.initServer(new DHCPServlet(), null); //Why not DHCPStaticServlet?
		        new Thread(dhcpServer).start();
		    } catch (DHCPServerInitException e) {
		        // die gracefully
		    	System.out.println("Error starting DHCP server");
		    	e.printStackTrace();
		    }
		}*/
			
		if(null != socket)
			socket.close();
		socket = new ServerSocket(port);
		
		Thread t = new Thread () {
			@Override
			public void run() {
				while(true) {
					try {
						Socket connection = socket.accept();
						System.out.println("About to add a new client!");						
						addClient(connection);
						System.out.println("Added a new client!");					
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
		/*if(null != dhcpServer) {
			dhcpServer.stopServer();
			dhcpServer = null;
		}*/
		if(null != socket) {
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
		Client c;
		while((c = clientQueue.poll()) == null) {/*System.out.println("Was null: " + clientQueue.size());*/}
		long l = c.send(msg,cb);
		clientQueue.put(c);
		return l;
	}
	
	@Override
	public void removeClient( Client ob) {
		clientQueue.remove(ob);
	}
	
	@Override
	public void closeAndRemove( Client ob) {
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
		return (Client[]) clientQueue.toArray();
	}
	
	@Override
	public void broadcast(Container c) {
		c.setPacketId(getNextId());
		Iterator<Client> iter = clientQueue.iterator();
		while(iter.hasNext())
			iter.next().broadcast(c);
	}
}