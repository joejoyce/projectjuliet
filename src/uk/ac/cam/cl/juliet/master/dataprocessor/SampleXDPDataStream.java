package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * @description This class creates a XDPDataStream from the provided
 * sample data on disk. The packets emitted by this class are timed
 * to simulate the actual timings of the packets sent in the data.
 * 
 * @author Scott Williams
 */
public class SampleXDPDataStream implements XDPDataStream {
	private long skipBoundary;

	private RandomAccessFile summaryFileHandle;
	private RandomAccessFile channelOneFileHandle;
	private RandomAccessFile channelTwoFileHandle;
	private RandomAccessFile channelThreeFileHandle;
	private HashMap<Integer, TimeStamp> timeStampBuffer = new HashMap<Integer, TimeStamp>();
	
	private long currentPacketCount = 0L;
	private long initialCallTimeNS;	
	private TimeStamp firstPacketTime;
	
	/**
	 * create and return a new SampleSDPDataStream to read in the four files of 
	 * sample data specified in the arguments.
	 * @param summaryFile		first file
	 * @param channelOne		second file
	 * @param channelTwo		third file
	 * @param channelThree		fourth file
	 * @param pSkipBoundary		If there is a time difference of more than 
	 * 			skipBoundary seconds between two consecutive packet timestamps,
	 * 			then the time between them is skipped to get a throughput and to 
	 * 			skip large periods of inactivity
	 * @throws IOException
	 */
	public SampleXDPDataStream(String summaryFile, String channelOne, 
			String channelTwo, String channelThree, float pSkipBoundary) 
					throws IOException {
		this.skipBoundary = (long) (1000000000*pSkipBoundary); //convert to nanoseconds
		this.summaryFileHandle = new RandomAccessFile(summaryFile, "r");
		this.channelOneFileHandle = new RandomAccessFile(channelOne, "r");
		this.channelTwoFileHandle = new RandomAccessFile(channelTwo, "r");
		this.channelThreeFileHandle = new RandomAccessFile(channelThree, "r");
		
		// Initialise buffer
		timeStampBuffer.put(new Integer(summaryFileHandle.hashCode()), null);
		timeStampBuffer.put(new Integer(channelOneFileHandle.hashCode()), null);
		timeStampBuffer.put(new Integer(channelTwoFileHandle.hashCode()), null);
		timeStampBuffer.put(new Integer(channelThreeFileHandle.hashCode()), null);
		
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
		
		if(currentPacketCount % 100 == 0)
			Debug.println("packet num: " + currentPacketCount);
		
		TimeStamp nextPacketData = getNextPacketDataStream();
		
		if(nextPacketData == null) {
			// End of all data channels
			return null;
		}
		
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
		long packetTimeDifferenceNS = packetTimeDifference*1000000000L + (nextPacketData.sendTimeNS - firstPacketTime.sendTimeNS);
		long systemDifferenceNS = packetTimeDifferenceNS - elapsedTimeNS;
		long systemDifferenceMS = TimeUnit.NANOSECONDS.toMillis(systemDifferenceNS);
		
		// Hacky solution to bursty datastream
		if(systemDifferenceNS > skipBoundary) {
			// If we have to wait more then the skipBoundary, skip ahead
			// Need to update internal timings to keep everything in sync
			initialCallTimeNS -= systemDifferenceNS;
			currentPacketCount ++;
			return new XDPRequest(fileData, toUnsignedInt(deliveryFlag));
		}
		
		if(systemDifferenceNS < 0) {
			// System is falling behind realtime
			Debug.println("System is " + (-systemDifferenceMS) + " milliseconds behind realtime stream");
		}
		else {
			/*initialCallTimeNS -= systemDifferenceNS;
			currentPacketCount ++;
			return new XDPRequest(fileData, toUnsignedInt(deliveryFlag));*/
			
			// System is ahead of realtime stream - wait for a bit
			Debug.println("System is " + systemDifferenceMS + " milliseconds ahead of realtime stream");
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
		
		addTimeStamp(times, summaryFileHandle);
		addTimeStamp(times, channelOneFileHandle);
		addTimeStamp(times, channelTwoFileHandle);
		addTimeStamp(times, channelThreeFileHandle);
		
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
		
		// Buffer value is now out of date
		timeStampBuffer.put(new Integer(times.get(0).f.hashCode()), null);		
		
		if(times.get(0).sendTime == Long.MAX_VALUE)
			return null;
		
		return times.get(0);		
	}
	
	/**
	 * Adds a TimeStamp object to the times ArrayList for a given file handle.
	 * Utilises the buffer to increase performance.
	 * @param times - The ArrayList of TimeStamps to add to, handle - the stream to read from
	 */
	private void addTimeStamp(ArrayList<TimeStamp> times, RandomAccessFile handle) throws IOException {		
		TimeStamp t = timeStampBuffer.get(new Integer(handle.hashCode()));
		if(t != null) {
			times.add(t);
		}
		else {		
			t = getTimeStamp(handle);
			timeStampBuffer.put(new Integer(handle.hashCode()), t);
			times.add(t);
		}		
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
		if(dataChannel.read(sendTimeBytes) == -1) {
			Debug.println("Datachanel: " + dataChannel.length() + ", reached end of file");
			// End of stream reached, make this TimeStamp unfavourable
			return new TimeStamp(Long.MAX_VALUE, Long.MAX_VALUE, dataChannel);
		}
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
