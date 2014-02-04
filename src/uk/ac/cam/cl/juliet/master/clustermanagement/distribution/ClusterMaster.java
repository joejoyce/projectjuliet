package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

//import org.dhcp4java.DHCPCoreServer;
//import org.dhcp4java.DHCPServerInitException;
//import org.dhcp4java.DHCPServlet;

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

public class ClusterMaster  {
	final static long queueFlushInteval = 500;
	//private static DHCPCoreServer dhcpServer = null;
	
	private ClusterMaster me = this;
	
	private ServerSocket socket = null;
	private AtomicLong nextId = new AtomicLong(0);
	
	private ClientLoadComparator clc = new ClientLoadComparator();
	private PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<Client>(16,clc);
	//TODO need to sort out the priorityblocking queue so that it can be efficiently reordered on one update
	//Make my own queue that also has fast random access so can be used for both ?
	
	/**
	 *  Add the client to the Cluster so that it can begin to recieve work
	 * @param skt The socket which has the client on the other end
	 */
	public void addClient(Socket skt) {
		Client c = new Client(skt,this);
		clientQueue.add(c); 
		//TODO sort this out
	}
	
	/**
	 * Start the cluster server so that Clients can be accepted - also starts the DHCP server for
	 * the network on which the Pis conect.
	 * @param port
	 * @throws IOException
	 */
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
						Client c = new Client(connection,me);
						clientQueue.add(c);
					} catch (IOException e) {
						System.out.println("There was an error establishing a connection and spawning a client");
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
	}
	
	/**
	 * Stop the server so that Clients recieve no more work and packets sent to the ClusterManger
	 * throw an exception - the DHCP server is also stopped.
	 */
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
	
	/**
	 * 	Send the Container for processing with no callback on reply, success is pretty much
	 * guaranteed as the ClusterManger will buffer and retry if no response is received.
	 * @param msg The Container to send to the cluster
	 * @throws NoClusterException In the case where the server is stopped or there are no Clients
	 */
	public long sendPacket(Container msg) throws NoClusterException {
		//Send the message - the process doesn't care about the reply
		return sendPacket(msg,(Callback)null);
	}
	
	/**
	 * 	Send the Container for processing and run the callback when a response is recieved,
	 * with the container as an argument, success is pretty much
	 * guaranteed as the ClusterManger will buffer and retry if no response is received.
	 * @param msg The Container to send to the cluster
	 * @param cb The callback to run on completion
	 * @throws NoClusterException In the case where the server is stopped or there are no Clients
	 */	
	public long sendPacket(Container msg, Callback cb) throws NoClusterException {
		Client c = clientQueue.poll();
		if(null == c) {
			throw new NoClusterException("The Pis have all gone :'(");
		}
		return c.send(msg);
	}
	/**
	 * Remove the client from this ClusterManager so that no more packets are sent to it
	 * @param ob The Client object to remove
	 */
	public void removeClient( Client ob) {
		clientQueue.remove(ob);
	}
	
	/**
	 * Remove the client from this ClusterManager so that no more packets are sent to it,
	 * but also close the connection to the Client.
	 * @param ob The Client to remove
	 */
	public void closeAndRemove( Client ob) {
		ob.closeClient();
		removeClient(ob);
	}
	
	public long getNextId() {
		return nextId.incrementAndGet();
	}

}