package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import uk.ac.cam.cl.juliet.common.Container;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;

/**
 * A callback which is used for the results of queries which are distributed to the cluster.
 * @author Dylan McDermott
 *
 */
public abstract class DistributedQueryCallback extends Callback {
	protected long total;
	protected long received = 0;

	/**
	 * Creates a new distributed query callback.
	 * @param total The total number of responses which are expected.
	 */
	public DistributedQueryCallback(long total) {
		this.total = total;
	}
	
	/**
	 * The method which is executed when a container is received from the cluster.
	 */
	public synchronized void callback(Container data) {
		received ++;
		processContainer(data);
		finished = received == total;
		if (received == total)
			this.notifyAll();
	}

	/**
	 * Blocks until the expected number of responses are received from the cluster.
	 */
	public synchronized void waitUntilDone() {
		while (total > received) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * Processes one of the responses from the cluster.
	 * @param data The response.
	 */
	protected abstract void processContainer(Container data);
}
