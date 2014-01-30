package uk.ac.cam.cl.juliet.common;

public abstract class Container {
	private long id;
	public Container(long id) {
		this.id = id;
	}
	public long getPacketId() {
		return this.id;
	}
	public void setPacketId(long id) {
		this.id = id;
	}
}
