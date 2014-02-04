package uk.ac.cam.cl.juliet.test.messagedecoding;

import java.io.IOException;
import java.io.PrintWriter;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.dataprocessor.SampleXDPDataStream;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;

public class PacketStatisticsCollector {
	private static XDPDataStream sXDPStream;
	private static XDPProcessor sXDPProcessor;
	private static MockDatabaseConnection sMockDB; 
	
	/**
	 * This will create a new SampleSDPDataStream to read in packets.
	 * These packets are then directly passed on to an XDPProcessor.
	 * The XDPProcessor uses a mocked up databaseConnection to write
	 * the decoded data into the DB. the mock databaseConnection will instead
	 * write the symbol mapping messages, a count on how many messages of each
	 * kind each stock symbol gets and a detailed log into .csv-files.
	 *  
	 * @param args
	 * 			The first argument is the path where the .csv-files will be stored.
	 * 			The second argument is the symbolIndex of the stock you want a 
	 * 			detailed log of.
	 */
	public static void main(String[] args) {
		try {
			String path = args[0];
			long symbolIndex = Integer.parseInt(args[1]);
			sXDPStream = new SampleXDPDataStream();
			sMockDB = 
					new MockDatabaseConnection(path, symbolIndex);
			
		} catch (IOException ioe) {
			System.err.println("Could not create XDPStream or MockDB");
			ioe.printStackTrace();
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("usage: PacketStatisticsCollection "
					+ "<path for output files> <symbolIndex>");
		}
		sXDPProcessor = new XDPProcessorUnit(sMockDB);
		
		readAndDecode();
		
	}
	private static void readAndDecode() {
		
		try {
			XDPRequest packet = sXDPStream.getPacket();
			while(packet != null) {
				sXDPProcessor.decode(packet);
				packet = sXDPStream.getPacket();
			}
			
		} catch (IOException e) {
			System.err.println("Could not get packet from XDPStream!");
			e.printStackTrace();
			return;
		}
		try {
			sMockDB.writeStatisticsToFile();
			sMockDB.writeSingleStockeLog();
		} catch (IOException e) {
			System.err.println("Could not write statistics to file!");
			e.printStackTrace();
		}
		System.out.println("Done!");
		
	}
	
}
