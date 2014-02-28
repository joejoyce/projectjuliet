package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;

public abstract class Callback {
	/**
	 * A boolean which indicates if the response has been received and processed.
	 */
	public volatile boolean finished = false;
	
	/**
	 * The method which is executed when a container is received from the cluster.
	 */
	public abstract void callback (Container data);
	
	/**
	 * Gets the status of the request.
	 * @return True if the response has been received and processed, otherwise false.
	 */
	public boolean isDone() {
		return finished;
	}
	
	/**
	 * Waits until a response if received and processed.
	 */
	public synchronized void waitUntilDone() {
		while(!isDone()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
