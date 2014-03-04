package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

/**
 * @description Global packet wrapper
 * Both XDPPacket and QueryPacket are subclasses
 * 
 * @author Scott Williams
 */
public abstract class Container implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private long id;
	private long timeStampS;

	private boolean highPri = false;
	
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
	public void setHighPriority(){
		highPri = true;
	}
	public boolean isHighPriority() {
		return highPri;
	}
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
