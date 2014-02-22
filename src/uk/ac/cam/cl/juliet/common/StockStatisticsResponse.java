package uk.ac.cam.cl.juliet.common;

public class StockStatisticsResponse extends QueryResponse {
	float lastTradePrice;
	long totalTradeVolume;
	float highestTradePrice;
	float lowestTradePrice;
	float change;
	float spread;

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
