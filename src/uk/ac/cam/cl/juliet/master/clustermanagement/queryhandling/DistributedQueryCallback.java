package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import uk.ac.cam.cl.juliet.common.Container;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;

public abstract class DistributedQueryCallback implements Callback {
	protected long total;
	protected long received = 0;

	public DistributedQueryCallback(long total) {
		this.total = total;
	}

	public synchronized void callback(Container data) {
		received ++;
		processContainer(data);
		if (received == total)
			this.notifyAll();
	}

	public synchronized void waitUntilDone() {
		while (total > received) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
	}
	
	protected abstract void processContainer(Container data);
}
