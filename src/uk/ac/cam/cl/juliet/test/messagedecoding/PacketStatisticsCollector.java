package uk.ac.cam.cl.juliet.test.messagedecoding;

import java.io.IOException;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.dataprocessor.SampleXDPDataStream;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;
/**
 * This class is used to test both the implementation of the XDPDataStream and the 
 * XDPProcessorUnit on the client to see whether the packets and messages are 
 * correctly read in and decoded.
 * It will produce .csv-files containing statistics about how many messages are send
 * per symbol index, a full list of the symbol mapping messages, and a full history
 * of all messages for one stock symbol.
 * 
 * @author Lucas Sonnabend
 * @see MockDatabaseConection
 */
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
	 * try {
			
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("Usage: <file1> <files2> <file3> <file4> <skipBoundary>");
			return;
		}
	 * 
	 * 
	 */
	public static void main(String[] args) {
		try {
			String path = args[0];
			long symbolIndex = Integer.parseInt(args[1]);
			String file1 = args[2];
			String file2 = args[3];
			String file3 = args[4];
			String file4 = args[5];
			float skipBoundary = Float.parseFloat(args[6]);
			sXDPStream = new SampleXDPDataStream(file1, file2, file3, file4, skipBoundary);
			sMockDB = 
					new MockDatabaseConnection(path, symbolIndex);
			
		} catch (IOException ioe) {
			System.err.println("Could not create XDPStream or MockDB");
			ioe.printStackTrace();
			return;
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("usage: PacketStatisticsCollection "
					+ "<path for output files> <symbolIndex> "
					+ "<file1> <files2> <file3> <file4> <skipBoundary>");
			e.printStackTrace();
			return;
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
