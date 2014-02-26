package uk.ac.cam.cl.juliet.common;
/**
 * This is the response message to a StockStatisticsRequest, returning the price of the last trade,
 * the total trade volume, the highest and the lowest trade price, change between the two 
 * last trade prices and the spread of the order book.
 * @author Lucas Sonnabend
 * @see StockStatisticsRequest
 */
public class StockStatisticsResponse extends QueryResponse {

	private static final long serialVersionUID = 1L;
	public float lastTradePrice;
	public long totalTradeVolume;
	public float highestTradePrice;
	public float lowestTradePrice;
	public float change;
	public float spread;
	/**
	 * Create a new StockStatisticsResponse
	 * @param id
	 * 		The packet ID of the corresponding request packet
	 * @param result
	 * 		A boolean indicator whether the query was processed successfully
	 * @param pLastTradePrice
	 * 		The price of the last trade
	 * @param pTotalTradeVolume
	 * 		The accumulated volume of all trades
	 * @param pHighestTradePrice
	 * 		The highest trade price seen so far
	 * @param pLowestTradePrice
	 * 		The lowest trade price seen so far
	 * @param pChange
	 * 		The absolute change between the last trade price and the second last 
	 * 		trade price
	 * @param pSpread
	 * 		The difference between the highest bid and the lowest offer in the orderbook
	 */
	public StockStatisticsResponse(long id, boolean result, float pLastTradePrice,
			long pTotalTradeVolume, float pHighestTradePrice, float pLowestTradePrice,
			float pChange, float pSpread) {
		super(id, result);
		this.lastTradePrice = pLastTradePrice;
		this.totalTradeVolume = pTotalTradeVolume;
		this.highestTradePrice = pHighestTradePrice;
		this.lowestTradePrice = pLowestTradePrice;
		this.change  = pChange;
		this.spread = pSpread;
	}

}
