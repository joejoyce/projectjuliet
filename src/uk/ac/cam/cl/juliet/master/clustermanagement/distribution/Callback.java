package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.Container;

public abstract class Callback {
	public volatile boolean finished = false;
	public abstract void callback (Container data);
	
	public synchronized void waitUntilDone() {
		while(!finished) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
