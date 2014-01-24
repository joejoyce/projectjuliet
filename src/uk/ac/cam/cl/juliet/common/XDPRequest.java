package uk.ac.cam.cl.juliet.common;

public class XDPRequest extends XDPPacket
{
	private int[] packetData;
	
	public XDPRequest(long id, int[] packetData)
	{
		super(id);
		this.packetData = packetData;
	}
	
	public int[] getPacketData()
	{
		return this.packetData;
	}
}