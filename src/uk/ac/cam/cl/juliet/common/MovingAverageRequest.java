package uk.ac.cam.cl.juliet.common;

/**
 * Requests the moving averages of the price for a single stock in a time period
 * 
 * @author Dylan McDermott
 * 
 */
public class MovingAverageRequest extends QueryPacket {
	private static final long serialVersionUID = 1L;
	private long symbolID;
	private long start;
	private long length;
	private long secondsPerAverage;

	/**
	 * Creates a new moving average request.
	 * 
	 * @param symbolID
	 *            The ID of the symbol to get stock prices for
	 * @param start
	 *            The start of the time period in seconds
	 * @param length
	 *            The length of the time period in seconds
	 * @param secondsPerAverage
	 *            The number of seconds which each average spans
	 */
	public MovingAverageRequest(long symbolID, long start, long length,
			long secondsPerAverage) {
		this.symbolID = symbolID;
		this.start = start;
		this.length = length;
		this.secondsPerAverage = secondsPerAverage;
		this.setHighPriority();
	}

	public long getSymbolId() {
		return symbolID;
	}

	public long getStart() {
		return start;
	}

	public long getLength() {
		return length;
	}

	public long getSecondsPerAverage() {
		return secondsPerAverage;
	}
}
