public class XDPRequest extends XDPPacket
{
	private byte[] packetData;
	
	public XDPRequest(long id, byte[] packetData)
	{
		super(id);
		this.packetData = packetData;
	}
	
	public byte[] getPacketData()
	{
		return this.packetData;
	}
}