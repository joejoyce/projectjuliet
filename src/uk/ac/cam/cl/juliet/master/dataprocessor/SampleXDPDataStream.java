package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * @description This class creates a XDPDataStream from the provided
 * sample data on disk. The packets emitted by this class are timed
 * to simulate the actual timings of the packets sent in the data.
 * 
 * @author Scott Williams
 */
public class SampleXDPDataStream implements XDPDataStream {
	private String summaryFile = "C:\\20111219-ARCA_XDP_IBF_1.dat";
	private String channelOne = "E:\\Juliet\\20111219-ARCA_XDP_IBF_2.dat";
	private String channelTwo = "E:\\Juliet\\20111219-ARCA_XDP_IBF_3.dat";
	private String channelThree = "E:\\Juliet\\20111219-ARCA_XDP_IBF_4.dat";	

	private RandomAccessFile summaryFileHandle;
	private RandomAccessFile channelOneFileHandle;
	private RandomAccessFile channelTwoFileHandle;
	private RandomAccessFile channelThreeFileHandle;
	
	private long currentPacketCount = 0L;
	private long initialCallTimeNS;	
	private TimeStamp firstPacketTime;
	
	public SampleXDPDataStream() throws IOException {
		this.summaryFileHandle = new RandomAccessFile(summaryFile, "r");
		this.channelOneFileHandle = new RandomAccessFile(channelOne, "r");
		this.channelTwoFileHandle = new RandomAccessFile(channelTwo, "r");
		this.channelThreeFileHandle = new RandomAccessFile(channelThree, "r");		
		this.firstPacketTime = getNextPacketDataStream();
		this.initialCallTimeNS = System.nanoTime();
	}

	/**
	 * Returns the next packet chosen by timestamp out of the 4 sample
	 * data files. This is a blocking IO call - a packet is returned 
	 * as close as possible to the relative timestamp of the packet
	 * within the stream. 
	 * @return The packet as a XDPRequest object
	 */
	@SuppressWarnings("static-access")
	public XDPRequest getPacket() throws IOException {
		TimeStamp nextPacketData = getNextPacketDataStream();
		RandomAccessFile nextPacketDataStream = nextPacketData.f;
		
		byte p1 = nextPacketDataStream.readByte();
		byte p2 = nextPacketDataStream.readByte();
		int packetSize = (toUnsignedInt(p2) << 8) | toUnsignedInt(p1);
		byte deliveryFlag = nextPacketDataStream.readByte();

		byte[] fileData = new byte[packetSize];

		fileData[0] = p1;
		fileData[1] = p2;
		fileData[2] = deliveryFlag;

		nextPacketDataStream.read(fileData, 3, packetSize - 3);
		
		long elapsedTimeNS = Math.abs(System.nanoTime() - initialCallTimeNS);
		long packetTimeDifference = nextPacketData.sendTime - firstPacketTime.sendTime;
		long packetTimeDifferenceNS = (long)(Math.pow(packetTimeDifference, 6)) + (nextPacketData.sendTimeNS - firstPacketTime.sendTimeNS);
		long systemDifferenceNS = packetTimeDifferenceNS - elapsedTimeNS;
		long systemDifferenceMS = TimeUnit.NANOSECONDS.toMillis(systemDifferenceNS);
		
		/*	System.out.println("Elapsed Time: " + elapsedTimeNS);
		System.out.println("packetTimeDifference: " + packetTimeDifference);
		System.out.println("packetTimeDifferenceNS: " + (nextPacketData.sendTimeNS - firstPacketTime.sendTimeNS));
		System.out.println("Current packet time in stream: " + java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(packetTimeDifferenceNS));	*/	
						
		if(systemDifferenceNS < 0) {
			// System is falling behind realtime
			System.out.println("System is " + (-systemDifferenceMS) + " milliseconds behind realtime stream");
		}
		else {
			// System is ahead of realtime stream - wait for a bit
			System.out.println("System is " + systemDifferenceMS + " milliseconds ahead of realtime stream");
			try {
				long milliSeconds = 0L;
				if(systemDifferenceNS > 999999) {
					systemDifferenceNS %= 1000000;
					milliSeconds = systemDifferenceMS;
				}			
				Thread.currentThread().sleep(milliSeconds, (int)systemDifferenceNS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		currentPacketCount ++;
		return new XDPRequest(fileData, toUnsignedInt(deliveryFlag));
	}
	
	/**
	 * Returns the datastream which contains the next packet
	 * according to the timestamps of the packets at the start of
	 * the file pointers.
	 * @return The datastream and timestamp wrapped as a TimeStamp object
	 */
	private TimeStamp getNextPacketDataStream() throws IOException {
		ArrayList<TimeStamp> times = new ArrayList<TimeStamp>();
		times.add(getTimeStamp(summaryFileHandle));
		times.add(getTimeStamp(channelOneFileHandle));
		times.add(getTimeStamp(channelTwoFileHandle));
		times.add(getTimeStamp(channelThreeFileHandle));
		
		Collections.sort(times, new Comparator<TimeStamp>() {
		    public int compare(TimeStamp t1, TimeStamp t2) {
		        if(t1.sendTime < t2.sendTime)
		        	return -1;
		        if(t1.sendTime > t2.sendTime)
		        	return 1;
		        
		        // Need to compare nanoseconds		        
		        if(t1.sendTimeNS < t2.sendTimeNS)
		        	return -1;
		        if(t1.sendTimeNS > t2.sendTimeNS)
		        	return 1;
		        
		        // Exactly equal!
		        return 0;		       
		    }
		});
		return times.get(0);		
	}
	
	/**
	 * Returns a TimeStamp object for the next packet given a specific file handle.
	 * The RandomAccessFile position is not affected by this method
	 * @param RandomAccessFile to read from
	 * @return The datastream and timestamp wrapped as a TimeStamp object
	 */
	private TimeStamp getTimeStamp(RandomAccessFile dataChannel) throws IOException {
		long currentPosition = dataChannel.getFilePointer();
		// Jump to the packet timestamp (seconds)
		dataChannel.seek(currentPosition + 8);
		
		byte[] sendTimeBytes = new byte[4];
		byte[] sendTimeNSBytes = new byte[4];		
		dataChannel.read(sendTimeBytes);
		dataChannel.read(sendTimeNSBytes);		

		long sendTime = littleEndianToLong(sendTimeBytes);
		long sendTimeNS = littleEndianToLong(sendTimeNSBytes);		
		
		// Move back
		dataChannel.seek(currentPosition);
		
		return new TimeStamp(sendTime, sendTimeNS, dataChannel);
	}
	
	/**
	 * Converts an array of bytes in little endian order to a long	 
	 * @param The array of bytes to convert 
	 * @return The datastream and timestamp wrapped as a TimeStamp object
	 */
	protected long littleEndianToLong(byte[] bytes) {
		long output = 0;
		int shift = 0;
		for (int i = 0; i < bytes.length; i++, shift += 8) {
			output |= (toUnsignedInt(bytes[i]) << shift);
		}
		return output;
	}
	
	/**
	 * Converts a signed byte to an unsigned int	 
	 * @param The singed byte
	 * @return The unsigned int
	 */
	private int toUnsignedInt(byte x) {
		return ((int) x) & 0xff;
	}
	
	public class TimeStamp {
		public long sendTime;
		public long sendTimeNS;	
		public RandomAccessFile f;
		
		public TimeStamp(long sendTime, long sendTimeNS, RandomAccessFile f) {
			this.sendTime = sendTime;
			this.sendTimeNS = sendTimeNS;	
			this.f = f;
		}
	}
}
