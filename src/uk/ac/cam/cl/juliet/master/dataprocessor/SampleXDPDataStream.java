package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class SampleXDPDataStream implements XDPDataStream {
	private String summaryFile = "C:\\20111219-ARCA_XDP_IBF_1.dat";
	private String channelOne = "E:\\Juliet\\20111219-ARCA_XDP_IBF_2.dat";
	private String channelTwo = "E:\\Juliet\\20111219-ARCA_XDP_IBF_3.dat";
	private String channelThree = "E:\\Juliet\\20111219-ARCA_XDP_IBF_4.dat";	

	private RandomAccessFile summaryFileHandle;
	private RandomAccessFile channelOneFileHandle;
	private RandomAccessFile channelTwoFileHandle;
	private RandomAccessFile channelThreeFileHandle;
	
	private long currentPacketID = 0L;
	private long initialCallTimeNS;	
	private TimeStamp firstPacketTime;
	
	public SampleXDPDataStream() throws IOException {
		this.summaryFileHandle = new RandomAccessFile(summaryFile, "r");
		this.channelOneFileHandle = new RandomAccessFile(channelOne, "r");
		this.channelTwoFileHandle = new RandomAccessFile(channelTwo, "r");
		this.channelThreeFileHandle = new RandomAccessFile(channelThree, "r");
		
		this.firstPacketTime = getNextPacketDataStream();
		this.initialCallTimeNS = System.currentTimeMillis();
	}

	// Blocking IO call - will only return a new packet when it's time for it
	public XDPPacket getPacket() throws IOException {
		TimeStamp nextPacketData = getNextPacketDataStream();
		RandomAccessFile nextPacketDataStream = nextPacketData.f;
		
		byte p1 = nextPacketDataStream.readByte();
		byte p2 = nextPacketDataStream.readByte();
		int packetSize = (toUnsignedInt(p2) << 8) | toUnsignedInt(p1);

		byte[] fileData = new byte[packetSize];

		fileData[0] = p1;
		fileData[1] = p2;

		nextPacketDataStream.read(fileData, 0, packetSize - 2);
		
		long elapsedTimeNS = Math.abs(System.nanoTime() - initialCallTimeNS);
		
		long packetTimeDifference = nextPacketData.sendTime - firstPacketTime.sendTime;
		long packetTimeDifferenceNS = (long)(Math.pow(packetTimeDifference, 9)) + (nextPacketData.sendTimeNS - firstPacketTime.sendTimeNS);
		
		long systemDifferenceNS = packetTimeDifferenceNS - elapsedTimeNS;
				
		
		if(systemDifferenceNS < 0) {
			// System is falling behind realtime
			System.out.println("System is " + (-systemDifferenceNS) + " nanoseconds behind realtime stream");
		}
		else {
			// System is ahead of realtime stream - wait for a bit
			// Hardcoded for now, will be able to change this in final version
			try {
				if(systemDifferenceNS > 999999) {
					// TODO convert to milliseconds keeping precision
				}
				Thread.currentThread().sleep(0L, (int)systemDifferenceNS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		return new XDPRequest(currentPacketID++, fileData);
	}
	
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
		        
		        //need to compare nanoseconds		        
		        if(t1.sendTimeNS < t2.sendTimeNS)
		        	return -1;
		        if(t1.sendTimeNS > t2.sendTimeNS)
		        	return 1;
		        
		        //Execally equal!
		        return 0;		       
		    }
		});
		
		System.out.println("0 sendtime: " + times.get(0).sendTime + ", nanos: " + times.get(0).sendTimeNS);
		System.out.println("1 sendtime: " + times.get(1).sendTime + ", nanos: " + times.get(1).sendTimeNS);
		System.out.println("2 sendtime: " + times.get(2).sendTime + ", nanos: " + times.get(2).sendTimeNS);
		System.out.println("3 sendtime: " + times.get(3).sendTime + ", nanos: " + times.get(3).sendTimeNS);
		
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
		
		return times.get(0);		
	}
	
	private TimeStamp getTimeStamp(RandomAccessFile dataChannel) throws IOException {
		long currentPosition = dataChannel.getFilePointer();
		//Jump to the packet timestamp (seconds)
		dataChannel.seek(currentPosition + 8);
		
		byte[] sendTimeBytes = new byte[4];
		byte[] sendTimeNSBytes = new byte[4];
		
		dataChannel.read(sendTimeBytes);
		dataChannel.read(sendTimeNSBytes);		

		long sendTime = littleEndianToLong(sendTimeBytes);
		long sendTimeNS = littleEndianToLong(sendTimeNSBytes);		
		
		//Move back
		dataChannel.seek(currentPosition);
		
		return new TimeStamp(sendTime, sendTimeNS, dataChannel);
	}
	
	protected long littleEndianToLong(byte[] bytes) {
		long output = 0;
		int shift = 0;

		for (int i = 0; i < bytes.length; i++, shift += 8) {
			output |= (toUnsignedInt(bytes[i]) << shift);
		}

		return output;
	}

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
