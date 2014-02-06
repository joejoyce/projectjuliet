package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;
/**
 * @description This is a class encapsulating a byte-array that represents
 * a XDP message. The byte-array will NOT contain the header of the message.
 * The header for all messages is 4 bytes long and contains the Message Size in bytes 
 * and the Message Type
 * 
 * @author lucas
 *
 */
public class Message extends DataWrapper{
	
	private int mMessageType;
	
	public Message(int pSize, byte[] pData) {
		super(pData);
		mSize = pSize;
		mMessageType = (int) readLong(2);		
	}
	
	/**
	 * @return code for the type of the message
	 *  	3 - symbol mapping
	 *  	33 - trade session change
	 *  	100 - order book add order
	 *  	101 - order book modify order
	 *  	102 - order book delete order
	 *  	103 - order book execution
	 *  	222 - trade correction
	 *  	223 - stock summary
	 */
	public int getMessageType() { return mMessageType;}

}
