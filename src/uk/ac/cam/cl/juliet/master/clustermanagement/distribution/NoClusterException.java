package uk.ac.cam.cl.juliet.master.clustermanagement.distribution;

public class NoClusterException extends Exception{

	private static final long serialVersionUID = 1L;

	public NoClusterException (String msg) {
		super(msg);
	}
}
