package src.uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.Iterator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;


import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.InFlightContainer;


final class ContainerTimeComparator implements Comparator<InFlightContainer> {
	public int compare(InFlightContainer o1, InFlightContainer o2){
		return (int) (o1.getDueTime() - o2.getDueTime() );
	}
}


public class ClusterMaster  {
	final static long queueFlushInteval = 500;
	
	
	private ServerSocket socket = null;
	private Thread listner = null; //, cleanup = null;
	
	private PriorityBlockingQueue<Client> clientQueue = new PriorityBlockingQueue<Client>();
	//TODO need to sort out the priorityblocking queue so that it can be efficiently reordered on one update
	//Make my own queue that also has fast random access so can be used for both
	public ClusterMaster() {
		
	}
	
	private void addClient(Socket skt) {
		Client c = new Client(skt,this);
		clientQueue.add(c); 
		//TODO sort this out
	}
	
	public void start(int port) throws IOException {
		if(null != socket)
			socket.close();
		socket = new ServerSocket(port);
		
		listner = new Thread () {
			@Override
			public void run() {
				while(true) {
					try {
						Socket connection = socket.accept();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};		
		
		
/*		cleanup = new Thread () {
			@Override
			public void run() {
				while(true) {
					Iterator<Client> iter = clientQueue.iterator();
					while(iter.hasNext()) {
						iter.next().tryFlushQueue();
					}
					try {
						Thread.sleep(queueFlushInteval);
					} catch (InterruptedException e) {
						return; //Stop cleaning the queues
					}
				}
			}
		};*/
	}
	
	public void stop() {
		if(null != listner)
			listner.interrupt(); //This should have the effect of cleaning up
		if(null != cleanup)
			cleanup.interrupt(); //Should stop it running
	}
		
	public void sendPacket(Container msg) {
		//Send the message - the process doesn't care about the reply
		sendPacket(msg,(Callback)null);
	}
	
	public void sendPacket(Container msg, Callback cb) {
		
	}
	
	public void removeClient( Client ob) {
		
	}

}
