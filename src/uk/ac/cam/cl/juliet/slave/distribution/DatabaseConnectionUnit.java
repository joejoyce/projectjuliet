package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.cam.cl.juliet.common.Debug;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private Connection connection;
	private PreparedStatement addOrderBatch;
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private int batchSize = 0;
	
	public DatabaseConnectionUnit(Connection c) throws SQLException {
		this.connection = c;
		this.addOrderBatch = connection.prepareStatement(
				"INSERT INTO order_book (order_id, symbol_id, price, volume, is_ask, placed_s, "
						  + "placed_seq_num, updated_s, updated_seq_num) "
						  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
					);	
		
		final Runnable executeBatch = new Runnable() {
			public void run() {
				try {
					System.out.println("About to execute batch size: " + batchSize);
					long then = System.nanoTime();
					addOrderBatch.executeBatch();
					
					synchronized(addOrderBatch) {
						addOrderBatch.close();
						addOrderBatch = connection.prepareStatement(
								"INSERT INTO order_book (order_id, symbol_id, price, volume, is_ask, placed_s, "
										  + "placed_seq_num, updated_s, updated_seq_num) "
										  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
									);	
					}
					
					double diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					System.out.println("Taken: " + diff);
					batchSize = 0;
				} catch (SQLException e) {					
					e.printStackTrace();
				}
			}
		};
		scheduler.scheduleAtFixedRate(executeBatch, 5, 5, TimeUnit.SECONDS);
	}

	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns, long symbolSeqNumber, long price, long volume, boolean isSell, int tradeSession, long packetTimestamp) throws SQLException {
		synchronized(addOrderBatch) {			
			addOrderBatch.setLong(1, orderID);
			addOrderBatch.setLong(2, symbolIndex);
			addOrderBatch.setLong(3, price);
			addOrderBatch.setLong(4, volume);
			addOrderBatch.setBoolean(5, isSell);
			addOrderBatch.setLong(6, packetTimestamp);
			addOrderBatch.setLong(7, symbolSeqNumber);
			addOrderBatch.setLong(8, packetTimestamp);
			addOrderBatch.setLong(9, symbolSeqNumber);
			addOrderBatch.addBatch();
		}
		batchSize ++;
	}

	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("UPDATE order_book SET price = ?, volume = ?, updated_s = ?, updated_seq_num = ? "
						+ "WHERE (order_id = ?) AND (symbol_id = ?)");
		statement.setLong(1, price);
		statement.setLong(2, volume);
		statement.setLong(3, packetTimestamp);
		statement.setLong(4, symbolSeqNumber);
		statement.setLong(5, orderID);
		statement.setLong(6, symbolIndex);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
		//batchSize ++;
	}

	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("UPDATE order_book SET volume = volume - ? "
						+ "WHERE (order_id = ?) AND (symbol_id = ?)");
		statement.setLong(1, volumeReduction);
		statement.setLong(2, orderID);
		statement.setLong(3, symbolIndex);
		//batchQuery.addBatch(statement.toString().split(":")[1]);	
		//batchSize ++;
	}

	@Override
	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("DELETE FROM order_book "
						+ "WHERE (order_id = ?) AND (symbol_id = ?)");
		statement.setLong(1, orderID);
		statement.setLong(2, symbolIndex);
		//batchQuery.addBatch(statement.toString().split(":")[1]);	
		//batchSize ++;
	}

	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("INSERT INTO trade (trade_id, symbol_id, price, volume, offered_s, "
						+ "offered_seq_num) " + "VALUES (?, ?, ?, ?, ?, ?)");
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, packetTimestamp);
		statement.setLong(6, symbolSeqNumber);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
		//batchSize ++;
		Debug.println("Added trade");
	}

	@Override
	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long highPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("INSERT INTO stock_summary (symbol_id, high_price, low_price, total_volume, "
						+ "updated_s, updated_ns) "
						+ "VALUES (?, ?, ?, ?, ?, ?)");
		statement.setLong(1, symbolIndex);
		statement.setLong(2, highPrice);
		statement.setLong(3, lowPrice);
		statement.setLong(4, totalVolume);
		statement.setLong(5, time_s);
		statement.setLong(6, time_ns);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
		//batchSize ++;
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("UPDATE trade SET trade_id = ?, symbol_id = ?, price = ?, volume = ? "
						+ "WHERE (trade_id = ?)");
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, originalTradeID);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
		//batchSize ++;
	}

	@Override
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException {
		// TODO implement method. Not sure how to add a source time reference to the database
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("DELETE FROM trade WHERE (trade_id = ?)");
		statement.setLong(1, tradeID);
		//batchQuery.addBatch(statement.toString().split(":")[1]);
		//batchSize ++;
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("INSERT INTO symbol (symbol_id, symbol, company_name, price_scale, open_price) VALUES (?,?,?,?,?)");
		statement.setLong(1, symbolIndex);
		statement.setString(2, symbol);
		statement.setString(3, "");
		statement.setLong(4, priceScaleCode);
		statement.setLong(5, prevClosingPrice);
		// batchQuery.addBatch(statement.toString().split(":")[1]);
	}

	@Override
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException {
		// TODO implement method. Not sure how to change over a trade session.
		// It will probably involve deleting all orders from that expire within
		// the current trade
		// session.
	}

	public ResultSet getTradesInTimeRangeForSymbol(long symbolID, int start, int end) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM trades WHERE symbol_id=? and offered_s>=? and offered_s<?");
		ResultSet result;
		try {
			statement.setLong(1, symbolID);
			statement.setInt(2, start);
			statement.setInt(3, end);
			result =  statement.executeQuery();
		} finally {
			statement.close();
		}
		return result;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}
