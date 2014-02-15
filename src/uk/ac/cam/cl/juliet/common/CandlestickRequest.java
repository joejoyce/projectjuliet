package uk.ac.cam.cl.juliet.common;

/**
 * A request which is sent to the slaves to compute part of the data for a
 * candlestick chart.
 * 
 * @author Dylan McDermott
 * 
 */
public class CandlestickRequest extends QueryPacket {

	private long symbolID;
	private int start;
	private int candlesticks;
	private int resolution;

	/**
	 * Creates a new candlestick request.
	 * 
	 * @param symbolID
	 *            The ID of the symbol to create a candlestick chart for.
	 * @param start
	 *            The start of the period of time which the
	 *            first candlestick covers in seconds
	 * @param candlesticks
	 *            The number of candlesticks to compute
	 * @param resolution
	 *            The number of seconds which each candlestick covers
	 */
	public CandlestickRequest(long symbolID, int start,
			int candlesticks, int resolution) {
		this.symbolID = symbolID;
		this.start = start;
		this.candlesticks = candlesticks;
		this.resolution = resolution;
	}

	public long getSymbolId() {
		return symbolID;
	}
	
	public int getStart() {
		return start;
	}

	public int getNumberOfCandlesticks() {
		return candlesticks;
	}

	public int getResolution() {
		return resolution;
	}
}
