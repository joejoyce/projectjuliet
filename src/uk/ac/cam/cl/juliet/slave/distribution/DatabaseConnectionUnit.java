package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private Connection connection;
	private Statement batchQuery;
	
	public DatabaseConnectionUnit(Connection c) throws SQLException {
		this.connection = c;
		this.batchQuery = connection.createStatement();
	}
	
	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell, 
			int tradeSession, long packetTimestamp) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO order_book (order_id, symbol_id, price, volume, is_ask, placed_s, "
			  + "placed_seq_num, updated_s, updated_seq_num) "
			  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
		);
		statement.setLong(1, orderID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setBoolean(5, isSell);
		statement.setLong(6, packetTimestamp);
		statement.setLong(7, symbolSeqNumber);
		statement.setLong(8, packetTimestamp);
		statement.setLong(9, symbolSeqNumber);
		batchQuery.addBatch(statement.toString().split(":")[1]);
	}
	
	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) 
					throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"UPDATE order_book SET price = ?, volume = ?, updated_s = ?, updated_seq_num = ? "
			  + "WHERE (order_id = ?) AND (symbol_id = ?)"
		);
		statement.setLong(1, price);
		statement.setLong(2, volume);
		statement.setLong(3, packetTimestamp);
		statement.setLong(4, symbolSeqNumber);
		statement.setLong(5, orderID);
		statement.setLong(6, symbolIndex);
		batchQuery.addBatch(statement.toString().split(":")[1]);
		
	}
	
	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"UPDATE order_book SET volume = volume - ? "
			  + "WHERE (order_id = ?) AND (symbol_id = ?)"
		);
		statement.setLong(1, volumeReduction);
		statement.setLong(2, orderID);
		statement.setLong(3, symbolIndex);
		batchQuery.addBatch(statement.toString().split(":")[1]);		
	}
	
	@Override
	public void deleteOrder(long orderID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long packetTimestamp) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"DELETE FROM order_book "
			  + "WHERE (order_id = ?) AND (symbol_id = ?)"
		);
		statement.setLong(1, orderID);
		statement.setLong(2, symbolIndex);
		batchQuery.addBatch(statement.toString().split(":")[1]);		
	}
	
	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns, 
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO trade (trade_id, symbol_id, price, volume, offered_s, "
			  +                    "offered_seq_num) "
			  + "VALUES (?, ?, ?, ?, ?, ?)"
		);
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, packetTimestamp);
		statement.setLong(6, symbolSeqNumber);
		batchQuery.addBatch(statement.toString().split(":")[1]);
		System.out.println("Added trade------------========");
	}

	@Override
	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long highPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO stock_summary (symbol_id, high_price, low_price, total_volume, "
			  +                            "updated_s, updated_ns) "
			  + "VALUES (?, ?, ?, ?, ?, ?)"
		);
		statement.setLong(1, symbolIndex);
		statement.setLong(2, highPrice);
		statement.setLong(3, lowPrice);
		statement.setLong(4, totalVolume);
		statement.setLong(5, time_s);
		statement.setLong(6, time_ns);
		batchQuery.addBatch(statement.toString().split(":")[1]);
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"UPDATE trade SET trade_id = ?, symbol_id = ?, price = ?, volume = ? "
			  + "WHERE (trade_id = ?)"
		);
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, originalTradeID);
		batchQuery.addBatch(statement.toString().split(":")[1]);
	}

	@Override
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException {
		// TODO implement method. Not sure how to add a source time reference to the database
		
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"DELETE FROM trade WHERE (trade_id = ?)"
		);
		statement.setLong(1, tradeID);
		batchQuery.addBatch(statement.toString().split(":")[1]);
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement(
				"INSERT INTO symbol (symbol_id, symbol, company_name, price_scale, open_price) VALUES (?,?,?,?,?)"
		);
		statement.setLong(1, symbolIndex);
		statement.setString(2, symbol);
		statement.setString(3, "");
		statement.setLong(4, priceScaleCode);
		statement.setLong(5, prevClosingPrice);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
	}

	@Override
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException {
		// TODO implement method. Not sure how to change over a trade session.
		// It will probably involve deleting all orders from that expire within the current trade
		// session.
	}
	
	@Override
	public void commit() throws SQLException{
		batchQuery.executeBatch();
		batchQuery.close();
		System.out.println("Executed batch");
		batchQuery = connection.createStatement();
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}
