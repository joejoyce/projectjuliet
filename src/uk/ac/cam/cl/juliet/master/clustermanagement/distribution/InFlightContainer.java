package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;

/**
 * This class holds the record of the container that was sent to the cluster.
 * This class is never sent but resides in the Client whilst it waits on a response.
 * @author joseph
 *
 */
public class InFlightContainer {
	private static long defaultTimeout = 5000000000L; //5 seconds I think - yea it is.
	private Container container;
	private long dueTime;
	private Callback callback = null;
	private boolean replyRecieved = false;
	
	/**
	 * 
	 * @return The callback that was specified - can be null
	 */
	public Callback getCallback() {
		return callback;
	}
	
	/**
	 * 
	 * @return The id of the encapsulated container
	 */
	public long getPacketId() {
		return container.getPacketId();
	}
	
	/**
	 * 
	 * @return returns true if a reply from the cluster to this Container has been recieved already
	 */
	public boolean hasReplyRecieved() {
		return replyRecieved;
	}
	
	/**
	 * This tells the IFC that a reply has been received
	 */
	public void setReplyRecieved() {
		replyRecieved = true;
	}
	
	/**
	 * This runs the callback for this container
	 * @param data The container that was returned in response to the query
	 */
	public void executeCallback(Container data) {
		if(null != callback ) 
			callback.callback(data);
	}
	
	/**
	 * 
	 * @return the time in nanoseconds that the packet should be back at - this is not a real time
	 */
	public long getDueTime(){
		return dueTime;
	}
	
	/**
	 * 
	 * @return The container that this object holds information on
	 */
	public Container getContainer() {
		return container;
	}
	InFlightContainer(Container container, long timeoutNs, Callback callback) {
		this.container = container;
		this.dueTime = System.nanoTime() + timeoutNs;
		this.callback = callback;
	}
	InFlightContainer(Container container) {
		this.container = container;
		this.callback = null;
		this.dueTime = System.nanoTime() + defaultTimeout;
	}
	InFlightContainer(Container container, Callback callback) {
		this.container = container;
		this.callback = callback;
		this.dueTime = System.nanoTime() + defaultTimeout;		
	}
}