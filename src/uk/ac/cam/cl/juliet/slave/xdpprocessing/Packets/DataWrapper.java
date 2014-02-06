package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public abstract class DataWrapper {
	private byte[] mData;
	protected int mDataPointer = 0;
	
	protected int mSize;
	
	/**
	 * create a new DataWrapper object, and read the size of the "packet"
	 * from the first two bytes.
	 * The Datapointer will point to 2 afterwards
	 * @param pData
	 */
	public DataWrapper(byte[] pData) {
		mData = pData;
	}
	/**
	 * check whether the datapointer reached the end of the byte array
	 * @return true if the datapointer reached the end or overshot,
	 * false otherwise
	 */
	public boolean datapointerAtEnd() {
		return (mDataPointer >= mSize);
	}

	/**
	 * Read up to the next four unsigned bytes and return them as a long.
	 * The datapointer advances length steps
	 * @param length the number of bytes to read
	 * @return the bytes as a long
	 */
	public long readLong(int length) {
		if(length > 4 || length < 0) 
			throw new IndexOutOfBoundsException();

		int[] unsignedBytes = new int[length];
		for (int i = 0; i < unsignedBytes.length; i++) {
			unsignedBytes[i] = readUnsignedByte();
		}

		return littleEndianToLong(unsignedBytes);
	}

	/**
	 * Read the next length bytes and return them as a string.
	 * The datapointer advances length steps.
	 * @param length number of bytes to read
	 * @return ASCII string of the bytes
	 */
	public String readString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(readChar());
		}
		return sb.toString();
	}
	/**
	 * converts an array of bytes in little endian into a long.
	 * Only the first four elements of the array are used
	 * @param bytes the input array
	 * @return long representation of the input
	 * @precondition 	all integers in bytes[] have to be non-negative and
	 * 					only their rightmost 8 bits may be non-zero
	 */
	protected long littleEndianToLong(int[] bytes) {
		long output = 0;
		int shift = 0;

		for (int i = 0; i < bytes.length; i++, shift += 8) {
			output |= (bytes[i] << shift);
		}

		return output;
	}
	/**
	 * Read a single unsigned byte and return it as a char.
	 * The datapointer advances by one.
	 * @return The char
	 */
	public char readChar() {
		// No need for bit masking - char is unsigned
		return (char) mData[mDataPointer++];
	}
	/**
	 * read a single unsigned byte and return it as an int.
	 * The datapointer advances by one.
	 * @return the value of the next unsigned byte, as an int
	 */
	public int readUnsignedByte() {
		//use a bitmask to turn negative values into corresponding positive values
		return ((int)mData[mDataPointer++]) & 0xFF;
	}
	/**
	 * Reads the next size of bytes and returns them as an array and
	 * the datapointer advances by size.
	 * @param size number of bytes to read.
	 * @return The read bytes as an array.
	 * @precondition there have to be at least size more bytes ahead of 
	 * the datapointer
	 */
	protected byte[] readBytes(int size) {
		byte result[] = new byte[size];
		for(int i=0;i<size;i++)
			result[i] = mData[mDataPointer++];
		return result;
	}
	
	public int getSize() { return this.mSize; }
	
}
