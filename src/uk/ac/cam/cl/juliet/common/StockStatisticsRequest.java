package uk.ac.cam.cl.juliet.common;
/**
 * This packet is used to ask a Pi to get get the price of the latest trade, the total
 * volume traded, the highest and lowest trade, the change between the last two trades
 * and the spread of the order book for one stock. The collection of these measurements
 * is called statistics.
 * 
 * @see StockStatisticsResponse
 * @author Lucas Sonnabend
 *
 */
public class StockStatisticsRequest extends QueryPacket {
	private static final long serialVersionUID = 1L;
	private long symbol_id;
	/**
	 * Creates a new StockStatisticsRequest to ask for the statistics of a stock
	 * @param symbolID
	 * 			The symbol index of the stock which statistics you are requesting
	 */
	public StockStatisticsRequest(long symbolID) {
		super();
		this.symbol_id = symbolID;
		this.setHighPriority();
	}
	/**
	 * Returns the symbol index of the stock which statistics you are requesting
	 * @return
	 */
	public long getSymbolID() {return this.symbol_id;}
}
