package uk.ac.cam.cl.juliet.common;
/**
 * @description Sent by the cluster to indicate a specific
 * XDPRequest has been completed 
 * 
 * @author Scott Williams
 */
public class XDPResponse extends XDPPacket {
	public XDPResponse(long id) {
		this.setPacketId(id);
	}
}