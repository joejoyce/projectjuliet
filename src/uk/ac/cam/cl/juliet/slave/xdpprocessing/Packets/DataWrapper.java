package uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets;

public abstract class DataWrapper {
	private byte[] mData;
	private int mDataPointer = 0;
	
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
	
	protected boolean datapointerAtEnd() {
		return (mDataPointer >= mSize);
	}
	protected void resetDataPointer() {
		mDataPointer = 0;
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
		return (char) mData[mDataPointer++];
	}

	protected int readUnsignedByte() {
		return mData[mDataPointer++];
	}
	
	protected byte[] readBytes(int size) {
		byte result[] = new byte[size];
		for(int i=0;i<size;i++)
			result[i] = mData[mDataPointer++];
		return result;
	}
	
}
