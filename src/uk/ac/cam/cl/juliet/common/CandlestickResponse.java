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
	private int start;
	private long[] open;
	private long[] close;
	private long[] high;
	private long[] low;
	private long[] volume;
	
	/**
	 * Creates a candlestick response.
	 * @param id The id of the request.
	 * @param start The time in seconds which the first candlestick begins at.
	 * @param open The open values of all of the candlesticks.
	 * @param close The close values of all of the candlesticks.
	 * @param high The high values of all of the candlesticks.
	 * @param low The low values of all of the candlesticks.
	 * @param volume The volume of trades in each of the candlesticks.
	 */
	public CandlestickResponse(long id, int start, long[] open, long[] close, long[] high, long[] low, long[] volume){
		super(id, true);
		this.start = start;
		this.open = open;
		this.close = close;
		this.high = high;
		this.low = low;
		this.volume = volume;
	}
	
	public int getStart() {
		return start;
	}
	
	public long[] getOpenValues() {
		return open;
	}
	
	public long[] getCloseValues() {
		return close;
	}
	
	public long[] getHighValues() {
		return high;
	}
	
	public long[] getLowValues() {
		return low;
	}
	
	public long[] getVolumeValues() {
		return volume;
	}
}
