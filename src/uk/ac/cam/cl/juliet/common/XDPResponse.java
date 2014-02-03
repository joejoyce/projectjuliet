package uk.ac.cam.cl.juliet.common;
/**
 * @description Sent by the cluster to indicate a specific
 * XDPRequest has been completed 
 * 
 * @author Scott Williams
 */
public class XDPResponse extends XDPPacket {
	boolean result;
	
	public XDPResponse(long id, boolean result) {
		this.setPacketId(id);
		this.result = result;
	}
	
	/*
	 * 
	 * @returns true if XDP packet was processed successfully, otherwise false.
	 */
	public boolean getResult() {
		return result;
	}
	
	/*
	 * Sets the result of processing the XDP packet.
	 */
	public void setResult(boolean value) {
		this.result = value;
	}
}