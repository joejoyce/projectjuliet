package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import java.io.IOException;
import java.net.Socket;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;

public interface ClusterMaster {

	/**
	 *  Add the client to the Cluster so that it can begin to recieve work
	 * @param skt The socket which has the client on the other end
	 */
	public abstract void addClient(Socket skt);

	/**
	 * Start the cluster server so that Clients can be accepted - also starts the DHCP server for
	 * the network on which the Pis conect.
	 * @param port
	 * @throws IOException
	 */
	public abstract void start(int port) throws IOException;

	/**
	 * Stop the server so that Clients recieve no more work and packets sent to the ClusterManger
	 * throw an exception - the DHCP server is also stopped.
	 */
	public abstract void stop();

	/**
	 * 	Send the Container for processing with no callback on reply, success is pretty much
	 * guaranteed as the ClusterManger will buffer and retry if no response is received.
	 * @param msg The Container to send to the cluster
	 * @throws NoClusterException In the case where the server is stopped or there are no Clients
	 */
	public abstract long sendPacket(Container msg) throws NoClusterException;

	/**
	 * 	Send the Container for processing and run the callback when a response is recieved,
	 * with the container as an argument, success is pretty much
	 * guaranteed as the ClusterManger will buffer and retry if no response is received.
	 * @param msg The Container to send to the cluster
	 * @param cb The callback to run on completion
	 * @throws NoClusterException In the case where the server is stopped or there are no Clients
	 */
	public abstract long sendPacket(Container msg, Callback cb)
			throws NoClusterException;

	/**
	 * Remove the client from this ClusterManager so that no more packets are sent to it
	 * @param ob The Client object to remove
	 */
	public abstract void removeClient(Client ob);

	/**
	 * Remove the client from this ClusterManager so that no more packets are sent to it,
	 * but also close the connection to the Client. Out of the two similar methods this is
	 * probably the one you want
	 * @param ob The Client to remove
	 */
	public abstract void closeAndRemove(Client ob);

	/**
	 * 
	 * @return The next unique id that should be used for sending packets to the cluster
	 */
	public abstract long getNextId();

	/**
	 * 
	 * @param key
	 * @return A String with the value of that setting
	 */
	public abstract String getSetting(String key);

	/**
	 * This method will set the key to the value specified and then broadcast the new
	 * configuration out to all the connected Clients
	 * @param key The string key 
	 * @param value The value to set it to
	 */
	public abstract void setSetting(String key, String value);

	/**
	 * Do not set properties on the object that this returns as it will cause a broadcast 
	 * of the update to the clients. That will only occur if you use the methods directly
	 * On the clusterMaster.
	 * @return The configurationPacket that this ClusterMasterUnit uses to keep track of config
	 */
	public abstract ConfigurationPacket getConfiguration();

	/**
	 * 
	 * @return An array of currently connected Clients
	 */
	public abstract Client[] listClients();
	
	/**
	 * @return Current system time
	 */
	public abstract long getTime();
	
	public abstract int getPacketThroughput();
	

	/**
	 * Sends the Container c out to all currently connected clients
	 * @param c
	 */
	public abstract void broadcast(Container c);
	
	/**
	 * Sends the container c to all connected clients with a callback.
	 * Warning the callbacks will run from different threads so make sure it's thread safe
	 * @param c
	 * @param cb
	 */
	public abstract void broadcast(Container c, Callback cb);

}