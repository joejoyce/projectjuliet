package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public class Packet extends DataWrapper{

	protected int deliveryFlag;
	protected int numberMsgs;
	protected long seqNum;
	protected long sendTime;
	protected long sendTimeNS;

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
	 * returns the delivery flag of the packet
	 * @return delivery flag as an int.
	 */
	public int getDeliveryFlag() {return this.deliveryFlag;}
	
	/**
	 * read the next message within the data array, wrap the data
	 * into a Message object and return this.
	 * The datapointer advances by the size of the next message
	 * @return the next message or null if you reached the end 
	 * 	of the packet
	 */
	public Message getNextMessage() {
		if(super.datapointerAtEnd()) {
			return null;
		} 
		int msgSize = (int) readLong(2);
		return new Message(msgSize, readBytes(msgSize));
	}
}