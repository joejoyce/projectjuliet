package uk.ac.cam.cl.juliet.common;
/**
 * @description Contains one packet from an XDPDataStream
 * These are sent to the cluster to be decoded
 * 
 * @author Scott Williams
 */
public class XDPRequest extends XDPPacket {
	private byte[] packetData;
	private int deliveryFlag;

	public XDPRequest(byte[] packetData, int deliveryFlag) {
		this.packetData = packetData;
		this.deliveryFlag = deliveryFlag;
	}

	public byte[] getPacketData() {
		return this.packetData;
	}

	public int getDeliveryFlag() {
		return deliveryFlag;
	}
}