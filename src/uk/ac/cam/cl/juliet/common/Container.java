package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

/**
 * @description Global packet wrapper
 * Both XDPPacket and QueryPacket are subclasses
 * 
 * @author Scott Williams
 */
public abstract class Container implements Serializable {
	private static final long serialVersionUID = 1L;
	private long id;
	private long timeStampS;

	
	public long getPacketId() {
		return this.id;
	}
	public void setPacketId(long id) {
		this.id = id;
	}	
	public long getTimeStampS() {
		return timeStampS;
	}
	public void setTimeStampS(long timeStampS) {
		this.timeStampS = timeStampS;
	}
}
