package uk.ac.cam.cl.juliet.Packets;

import java.io.RandomAccessFile;
import java.io.IOException;

public abstract class Packet
{
	private RandomAccessFile file;
	
	protected long packetSize;
	protected long deliveryFlag;
	protected long numberMsgs;
	protected long seqNum;
	protected long sendTime;
	protected long sendTimeNS;
	
	protected long msgSize;
	protected long msgType;
	
	public Packet(RandomAccessFile file) throws IOException
	{
		this.file = file;
		
		this.packetSize = readLong(2);
		this.deliveryFlag = file.read();
		this.numberMsgs = file.read();
		this.seqNum = readLong(4);
		this.sendTime = readLong(4);	
		this.sendTimeNS = readLong(4);		
		
		this.msgSize = readLong(2);	
		this.msgSize = readLong(2);
	}
	
	protected long readLong(int length) throws IOException
	{
		int[] unsignedBytes = new int[length];
		for(int i = 0; i < unsignedBytes.length; i ++)
		{
			unsignedBytes[i] = file.readUnsignedByte();
		}
		
		return littleEndianToLong(unsignedBytes);
	}
	
	protected String readString(int length) throws IOException
	{
		byte[] bytes = new byte[length];
		file.read(bytes, 0, length);
		return new String(bytes);
	}
	
	protected long littleEndianToLong(int[] bytes)
	{
		long output = 0;
		int shift = 0;
		
		for(int i = 0; i < bytes.length; i ++, shift += 8)
		{
			output |= (bytes[i] << shift);
		}
		
		return output;
	}	
}