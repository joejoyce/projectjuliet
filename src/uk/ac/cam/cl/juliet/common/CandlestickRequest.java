package uk.ac.cam.cl.juliet.common;

/**
 * A request which is sent to the slaves to compute part of the data for a
 * candlestick chart.
 * 
 * @author Dylan McDermott
 * 
 */
public class CandlestickRequest extends QueryPacket {
	private static final long serialVersionUID = 1L;
	private long symbolID;
	private long start;
	private int resolution;

	/**
	 * Creates a new candlestick request.
	 * 
	 * @param symbolID
	 *            The ID of the symbol to create a candlestick chart for.
	 * @param start
	 *            The start of the period of time which the first candlestick
	 *            covers in seconds
	 * @param resolution
	 *            The number of seconds which the candlestick covers
	 */
	public CandlestickRequest(long symbolID, long start, int resolution) {
		this.symbolID = symbolID;
		this.start = start;
		this.resolution = resolution;
	}

	public long getSymbolId() {
		return symbolID;
	}
	public long getStart() {
		return start;
	}

	public int getResolution() {
		return resolution;
	}
}
