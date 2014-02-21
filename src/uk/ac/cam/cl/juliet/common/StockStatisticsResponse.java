package uk.ac.cam.cl.juliet.common;

public class StockStatisticsResponse extends QueryResponse {
	long lastTradePrice;
	long totalTradeVolume;
	long highestTradePrice;
	long lowestTradePrice;
	long change;
	long spread;

	public StockStatisticsResponse(long id, boolean result, long pLastTradePrice,
			long pTotalTradeVolume, long pHighestTradePrice, long pLowestTradePrice,
			long pChange, long pSpread) {
		super(id, result);
		this.lastTradePrice = pLastTradePrice;
		this.totalTradeVolume = pTotalTradeVolume;
		this.highestTradePrice = pHighestTradePrice;
		this.lowestTradePrice = pLowestTradePrice;
		this.change  = pChange;
		this.spread = pSpread;
	}

}
