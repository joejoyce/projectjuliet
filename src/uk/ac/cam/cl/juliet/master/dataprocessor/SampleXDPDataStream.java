package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.RandomAccessFile;
import java.io.IOException;

public class SampleXDPDataStream implements XDPDataStream {
	private String dataSetPath = "C:\\20111219-ARCA_XDP_IBF_1.dat";

	private RandomAccessFile sampleData;
	private long currentPacketID = 0L;

	public SampleXDPDataStream() throws IOException {
		this.sampleData = new RandomAccessFile(dataSetPath, "r");
	}

	public XDPPacket getPacket() throws IOException {
		byte p1 = sampleData.readByte();
		byte p2 = sampleData.readByte();
		int packetSize = (toUnsignedInt(p2) << 8) | toUnsignedInt(p1);

		byte[] fileData = new byte[packetSize];

		fileData[0] = p1;
		fileData[1] = p2;

		sampleData.read(fileData, 0, packetSize - 2);

		return new XDPRequest(currentPacketID++, fileData);
	}

	private int toUnsignedInt(byte x) {
		return ((int) x) & 0xff;
	}

	// Won't be used with the static sample dataset
	public XDPPacket requestPacket(long packetID) {
		return null;
	}
}