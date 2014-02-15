package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

/**
 * @description Contains one packet from an XDPDataStream
 * These are sent to the cluster to be decoded
 * 
 * @author Scott Williams
 */
public class XDPRequest extends XDPPacket  implements Serializable {
	private static final long serialVersionUID = 1L;
	private byte[] packetData;
	private int deliveryFlag;
	
	public XDPRequest(byte[] packetData, int deliveryFlag, long timeS) {
		this.packetData = packetData;
		this.deliveryFlag = deliveryFlag;
		setTimeStampS(timeS);
	}

	public byte[] getPacketData() {
		return this.packetData;
	}

	public int getDeliveryFlag() {
		return deliveryFlag;
	}
}