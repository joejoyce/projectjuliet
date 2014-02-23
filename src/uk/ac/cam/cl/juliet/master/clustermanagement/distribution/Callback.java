package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;

public abstract class Callback {
	public volatile boolean finished = false;
	public abstract void callback (Container data);
	
	public boolean isDone() {
		return finished;
	}
	
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
