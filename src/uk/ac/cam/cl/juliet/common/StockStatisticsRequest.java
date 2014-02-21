package uk.ac.cam.cl.juliet.common;

public class StockStatisticsRequest extends QueryPacket {
	private static final long serialVersionUID = 1L;
	private long symbol_id;
	
	public StockStatisticsRequest(long symbolID) {
		super();
		this.symbol_id = symbolID;
	}
	public long getSymbolID() {return this.symbol_id;}
}
