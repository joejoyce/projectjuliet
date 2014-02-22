package uk.ac.cam.cl.juliet.common;

public class StockStatisticsResponse extends QueryResponse {

	private static final long serialVersionUID = 1L;
	public float lastTradePrice;
	public long totalTradeVolume;
	public float highestTradePrice;
	public float lowestTradePrice;
	public float change;
	public float spread;

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
