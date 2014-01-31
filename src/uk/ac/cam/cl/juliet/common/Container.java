package uk.ac.cam.cl.juliet.common;
/**
 * @description Global packet wrapper
 * Both XDPPacket and QueryPacket are subclasses
 * 
 * @author Scott Williams
 */
public abstract class Container {
	private long id;
	
	public long getPacketId() {
		return this.id;
	}
	public void setPacketId(long id) {
		this.id = id;
	}
}
