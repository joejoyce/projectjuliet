package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private Connection connection;
	private ConcurrentLinkedQueue<PreparedStatement> batchQuery = new ConcurrentLinkedQueue<PreparedStatement>();
	
	public DatabaseConnectionUnit(Connection c) throws SQLException {
		this.connection = c;
	}
	
	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell, 
			int tradeSession, long packetTimestamp) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO order_book (order_id, symbol_id, price, volume, is_ask, placed_s,"
			  + "placed_seq_num, updated_s, updated_seq_num)"
			  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
		);
		statement.setLong(1, orderID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setBoolean(5, isSell);
		statement.setLong(6, packetTimestamp); //use timestamp in secs from packet
		statement.setLong(7, symbolSeqNumber);
		statement.setLong(8, packetTimestamp); //use timestamp in secs from packet
		statement.setLong(9, symbolSeqNumber);
		batchQuery.add(statement);
	}
	
	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) 
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
			long symbolSeqNumber, long packetTimestamp) throws SQLException {
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

	@Override
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void commit() throws SQLException{
		PreparedStatement ps;
		System.out.println("About to comit");
		while( (ps = batchQuery.poll()) != null) {
			System.out.println("Commiting one part----------");
			ps.execute();
		}
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}
