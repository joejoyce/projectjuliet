package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.net.Socket;
import java.io.IOException;

public class DataProcessor
{
	private XDPDataStream dataStream;
	private Socket distributor;
	
	public DataProcessor(XDPDataStream dataStream)
	{
		this.dataStream = dataStream;
	}
	
	public void start()
	{
		int totalPackets = 0;
		XDPPacket packet = null;
		do
		{
			try
			{
				packet = dataStream.getPacket();
			}
			catch(IOException e)
			{
				System.err.println("Datastream error");
			}
			
			//might be a good idea to fire off a thread here:		
			sendPacket(packet);
			totalPackets ++;	
			
			if(totalPackets % 1000 == 0)
			{
				System.out.println("packets: " + totalPackets);
			}
		} while(packet != null);
		System.out.println("Number of packets is!!!!: " + totalPackets);
	}
	
	private void sendPacket(XDPPacket packet)
	{
		//timing stuff can go here
		//might be worth doing this in a seperate thread			
	}
	
	public static void main(String[] args) throws IOException
	{
		SampleXDPDataStream ds = new SampleXDPDataStream();
		DataProcessor dp = new DataProcessor(ds);
		dp.start();
	}
}