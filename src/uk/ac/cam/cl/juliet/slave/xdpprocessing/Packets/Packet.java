package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public class Packet {
	private int[] data;
	private int dataPointer = 0;

	protected long packetSize;
	protected long deliveryFlag;
	protected long numberMsgs;
	protected long seqNum;
	protected long sendTime;
	protected long sendTimeNS;

	protected long msgSize;
	protected long msgType;

	public Packet(int[] data) {
		this.data = data;

		this.packetSize = readLong(2);
		this.deliveryFlag = (long) readUnsignedByte();
		this.numberMsgs = (long) readUnsignedByte();
		this.seqNum = readLong(4);
		this.sendTime = readLong(4);
		this.sendTimeNS = readLong(4);

		this.msgSize = readLong(2);
		this.msgType = readLong(2);
	}

	protected long readLong(int length) {
		int[] unsignedBytes = new int[length];
		for (int i = 0; i < unsignedBytes.length; i++) {
			unsignedBytes[i] = readUnsignedByte();
		}

		return littleEndianToLong(unsignedBytes);
	}

	protected String readString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(readChar());
		}
		return sb.toString();
	}

	protected long littleEndianToLong(int[] bytes) {
		long output = 0;
		int shift = 0;

		for (int i = 0; i < bytes.length; i++, shift += 8) {
			output |= (bytes[i] << shift);
		}

		return output;
	}

	protected char readChar() {
		return (char) data[dataPointer++];
	}

	protected int readUnsignedByte() {
		return data[dataPointer++];
	}
}