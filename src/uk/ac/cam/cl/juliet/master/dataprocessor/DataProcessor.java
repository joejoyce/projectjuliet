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
				break;
			}
			
			//might be a good idea to fire off a thread here:		
			sendPacket(packet);
		} while(packet != null);
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