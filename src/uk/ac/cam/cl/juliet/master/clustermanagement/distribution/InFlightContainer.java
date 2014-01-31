package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;

public class InFlightContainer {
	private static long defaultTimeout = 5000000000L; //5 seconds I think - yea it is.
	private Container container;
	private long dueTime;
	private Callback callback = null;
	private boolean replyRecieved = false;
	
	public Callback getCallback() {
		return callback;
	}
	public long getPacketId() {
		return container.getPacketId();
	}
	public boolean hasReplyRecieved() {
		return replyRecieved;
	}
	public void setReplyRecieved() {
		replyRecieved = true;
	}
	
	public void executeCallback(Container data) {
		if(null != callback ) 
			callback.callback(data);
	}
	public long getDueTime(){
		return dueTime;
	}
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
