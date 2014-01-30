package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public class Packet extends DataWrapper{

	protected long deliveryFlag;
	protected long numberMsgs;
	protected long seqNum;
	protected long sendTime;
	protected long sendTimeNS;

	public Packet(byte[] data) {
		super(data);
		
		super.mSize = (int) readLong(2);

		this.deliveryFlag = (long) readUnsignedByte();
		this.numberMsgs = (long) readUnsignedByte();
		this.seqNum = readLong(4);
		this.sendTime = readLong(4);
		this.sendTimeNS = readLong(4);

		//this.msgSize = readLong(2);
		//this.msgType = readLong(2);
	}

	
	
	public Message getNextMessage() {
		if(super.datapointerAtEnd()) {
			return null;
		} 
		int msgSize = (int) readLong(2);
		return new Message(msgSize, readBytes(msgSize));
	}
}