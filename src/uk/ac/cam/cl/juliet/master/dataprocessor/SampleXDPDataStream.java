package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.RandomAccessFile;
import java.io.IOException;

public class SampleXDPDataStream implements XDPDataStream
{
	private String dataSetPath = "C:\\20111219-ARCA_XDP_IBF_1.dat";
	
	private RandomAccessFile sampleData;
	private long fileLength = 0L;
	private long currentPacketID = 0L;
	
	
	public SampleXDPDataStream() throws IOException
	{
		this.sampleData = new RandomAccessFile(dataSetPath, "r");
		this.fileLength = sampleData.length();
	}
	
	public XDPPacket getPacket() throws IOException
	{
		int p1 = sampleData.readUnsignedByte(); 
		int p2 = sampleData.readUnsignedByte();
		int packetSize = (p2 << 8) | p1;
		
		int[] packetData = new int[packetSize];
		
		packetData[0] = p1;
		packetData[1] = p2;		
		
		for(int i = 2; i < packetSize; i ++)
		{
			packetData[i] = sampleData.readUnsignedByte();
		}
		
		return new XDPRequest(currentPacketID++, packetData);
	}
	
	//Won't be used with the static sample dataset
	public XDPPacket requestPacket(long packetID) {return null;}
}