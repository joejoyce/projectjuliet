package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public class Packet extends DataWrapper {

	protected int deliveryFlag;
	protected int numberMsgs;
	protected long seqNum;
	protected long sendTime;
	protected long sendTimeNS;

	/**
	 * Create a packet object that wraps the data of a packet
	 * 
	 * It will read the header of the packet from the data, the data pointer will then be at
	 * position 16 in the data array.
	 * 
	 * @param data
	 */
	public Packet(byte[] data) {
		super(data);
		super.mSize = (int) readLong(2);

		deliveryFlag = readUnsignedByte();
		numberMsgs = readUnsignedByte();
		seqNum = readLong(4);
		sendTime = readLong(4);
		sendTimeNS = readLong(4);
	}
	
	/**
	 * Return the delivery flag of the packet
	 * 
	 * @return delivery flag as an int.
	 */
	public int getDeliveryFlag() { return this.deliveryFlag; }
	
	/**
	 * Return the number of messages in this packet
	 * 
	 * @return number of messages (int)
	 */
	public int getNumberOfMsgs() { return this.numberMsgs; }
	
	/**
	 * Return sequence number of this packet
	 * 
	 * @return sequence number (long)
	 */
	public long getSequenceNumber() { return this.seqNum; }
	
	/**
	 * Return the send time (seconds since Unix Epoch) of this packet
	 * 
	 * @return send time of packet (long)
	 */
	public long getTimestamp() { return this.sendTime; }
	/**
	 * Return the send time in nanoseconds of this packet
	 * 
	 * @return nanosecond send time of packet (long)
	 */
	public long getTimestampNS() { return this.sendTimeNS; }
	
	/**
	 * Read the next message within the data array
	 * 
	 * This wraps the data into a Message object and returns the message object.
	 * The data pointer advances by the size of the next message
	 * 
	 * @return the next message or null if you reached the end of the packet
	 */
	public Message getNextMessage() {
		if(super.datapointerAtEnd()) {
			return null;
		} 
		int msgSize = (int) readLong(2);
		return new Message(msgSize, readBytes(msgSize-2));
	}
}