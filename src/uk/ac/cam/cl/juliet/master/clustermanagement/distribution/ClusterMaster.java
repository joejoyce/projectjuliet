package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.dhcp4java.DHCPCoreServer;
import org.dhcp4java.DHCPServerInitException;
import org.dhcp4java.DHCPServlet;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;


final class ClientLoadComparator implements Comparator<Client> {
	public int compare(Client o1, Client o2){
		return (int) (o1.getCurrentWork() - o2.getCurrentWork() );
	}
}

public class ClusterMaster  {
	final static long queueFlushInteval = 500;
	private static DHCPCoreServer dhcpServer = null;
	
	private ClusterMaster me = this;
	
	private ServerSocket socket = null;
	
	private ClientLoadComparator clc = new ClientLoadComparator();
	private PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<Client>(16,clc);
	//TODO need to sort out the priorityblocking queue so that it can be efficiently reordered on one update
	//Make my own queue that also has fast random access so can be used for both ?
	
	public void addClient(Socket skt) {
		Client c = new Client(skt,this);
		clientQueue.add(c); 
		//TODO sort this out
	}
	
	public void start(int port) throws IOException {
		if(null == dhcpServer) {
		    try {
		    	dhcpServer = DHCPCoreServer.initServer(new DHCPServlet(), null); //Why not DHCPStaticServlet?
		        new Thread(dhcpServer).start();
		    } catch (DHCPServerInitException e) {
		        // die gracefully
		    	System.out.println("Error starting DHCP server");
		    	e.printStackTrace();
		    }
		}
			
		if(null != socket)
			socket.close();
		socket = new ServerSocket(port);
		
		new Thread () {
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
	}
	
	public void stop() {
		if(null != dhcpServer) {
			dhcpServer.stopServer();
			dhcpServer = null;
		}
		if(null != socket) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} //This should have the effect of cleaning up
		}
		socket = null;
	}
		
	public void sendPacket(Container msg) throws NoClusterException {
		//Send the message - the process doesn't care about the reply
		sendPacket(msg,(Callback)null);
	}
	
	public void sendPacket(Container msg, Callback cb) throws NoClusterException {
		Client c = clientQueue.poll();
		if(null == c) {
			throw new NoClusterException("The Pis have all gone :'(");
		}
		c.send(msg);
	}
	
	public void removeClient( Client ob) {
		clientQueue.remove(ob);
	}
	
	public void closeAndRemove( Client ob) {
		ob.closeClient();
		removeClient(ob);
	}

}
