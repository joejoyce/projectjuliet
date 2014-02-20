package uk.ac.cam.cl.juliet.common;

/**
 * A response sent from the slaves to the master containing data for a
 * candlestick chart.
 * 
 * @author Dylan McDermott
 * 
 */
public class CandlestickResponse extends QueryResponse {
	private static final long serialVersionUID = 1L;
	private long start;
	private long open;
	private long close;
	private long high;
	private long low;
	private long volume;
	private long timeS;
	
	/**
	 * Creates a candlestick response.
	 * @param id The id of the request.
	 * @param start The time in seconds which the candlestick begins at.
	 * @param open The open value of the candlestick.
	 * @param close The close value of the candlestick.
	 * @param high The high value of the candlestick.
	 * @param low The low value of the candlestick.
	 * @param volume The volume of trades in the candlestick.
	 */
	public CandlestickResponse(long id, long start, long open, long close, long high, long low, long volume, long timeS) {
		super(id, true);
		this.start = start;
		this.open = open;
		this.close = close;
		this.high = high;
		this.low = low;
		this.volume = volume;
		this.timeS = timeS;
	}
	
	public long getStart() {
		return start;
	}
	
	public long getOpenValue() {
		return open;
	}
	
	public long getCloseValue() {
		return close;
	}
	
	public long getHighValue() {
		return high;
	}
	
	public long getLowValue() {
		return low;
	}
	
	public long getVolumeValue() {
		return volume;
	}
	
	public long getTimeStampS() {
		return timeS;
	}
}
