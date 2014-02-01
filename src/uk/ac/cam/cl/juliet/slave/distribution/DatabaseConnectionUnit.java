package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private Connection connection; 
	
	public DatabaseConnectionUnit() throws SQLException {
		//TODO create/or pass on connection to Database
	}
	
	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell, 
			int tradeSession) throws SQLException {
		//TODO add a new order to the order-book of the stock 
		// indexed by symbolIndex
	}
	
	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell) 
					throws SQLException {
		//TODO update the order in the order-book 
		// specified by orderID and symbolIndex
	}
	
	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction) {
		//TODO get the order book entry of the order with the current volume
		// and update it to current volume - volumeReduction
	}
	
	@Override
	public void deleteOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber) throws SQLException {
		//TODO delete the order from the order book 
		// specified by orderID and symbolIndex
	}
	
	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long prize, long volume) throws SQLException {
		//TODO add a new trade to the database
	}

	@Override
	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long lighPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException {
		// TODO Auto-generated method stub
		
	}
}
