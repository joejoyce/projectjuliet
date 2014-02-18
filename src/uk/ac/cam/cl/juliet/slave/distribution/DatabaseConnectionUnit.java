package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private Connection connection;
	private PreparedStatement addOrderBatch;
	private PreparedStatement addTradeBatch;
	private PreparedStatement deleteOrderBatch;
	private PreparedStatement modifyOrderBatch;
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private int batchSize = 0;
	private int delete = 0;

	public DatabaseConnectionUnit(Connection c) throws SQLException {
		this.connection = c;
		this.addOrderBatch = connection.prepareStatement("INSERT INTO order_book VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		this.addTradeBatch = connection.prepareStatement("INSERT INTO trade VALUES (?, ?, ?, ?, ?, ?, NULL, NULL)");
		//this.deleteOrderBatch = connection.prepareStatement("UPDATE order_book SET is_deleted=1 WHERE (order_id = ?) AND (symbol_id = ?)");
		this.deleteOrderBatch = connection.prepareStatement("DELETE FROM order_book WHERE (order_id = ?) AND (symbol_id = ?)");
		this.modifyOrderBatch = connection.prepareStatement("UPDATE order_book SET price = ?, volume = ?, updated_s = ?, updated_seq_num = ? WHERE (order_id = ?) AND (symbol_id = ?)");
		
		final Runnable executeBatch = new Runnable() {
			public void run() {
				try {
					System.out.println("Total batch size: " + batchSize);
					long start = System.nanoTime();
					
					System.out.println("About to execute addOrder batch");
					long then = System.nanoTime();
					synchronized (addOrderBatch) {
						addOrderBatch.executeBatch();
						addOrderBatch.clearBatch();
					}
					double diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					System.out.println("Taken: " + diff);
		
					System.out.println("About to execute addTrade batch");
					then = System.nanoTime();
					synchronized (addTradeBatch) {
						addTradeBatch.executeBatch();
						addTradeBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					System.out.println("Taken: " + diff);
		
					System.out.println("About to execute deleteOrder batch: " + delete);
					delete = 0;
					then = System.nanoTime();
					synchronized (deleteOrderBatch) {
						deleteOrderBatch.executeBatch();
						deleteOrderBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					System.out.println("Taken: " + diff);
					
					System.out.println("About to execute modifyOrderBatch batch");
					then = System.nanoTime();
					synchronized(modifyOrderBatch) {
						modifyOrderBatch.executeBatch();
						modifyOrderBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					System.out.println("Taken: " + diff);				
					
					batchSize = 0;
					
					long totalTaken = Math.abs(System.nanoTime() - start);
					totalTaken /= 1000000;
					System.out.println("Total time taken: " + totalTaken);
					System.out.println("-------------------------------------");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};
		scheduler.scheduleAtFixedRate(executeBatch, 5, 1, TimeUnit.SECONDS);
	}

	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns, long symbolSeqNumber, long price, long volume, boolean isSell, int tradeSession, long packetTimestamp) throws SQLException {
		synchronized (addOrderBatch) {
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
		batchSize++;
	}

	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns, long symbolSeqNumber, long price, long volume, boolean isSell, long packetTimestamp) throws SQLException {
		synchronized(modifyOrderBatch) {
			modifyOrderBatch.setLong(1, price);
			modifyOrderBatch.setLong(2, volume);
			modifyOrderBatch.setLong(3, packetTimestamp);
			modifyOrderBatch.setLong(4, symbolSeqNumber);
			modifyOrderBatch.setLong(5, orderID);
			modifyOrderBatch.setLong(6, symbolIndex);
			modifyOrderBatch.addBatch();
		}
		batchSize++;
	}

	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns, long symbolSeqNumber, long volumeReduction) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("UPDATE order_book SET volume = volume - ? WHERE (order_id = ?) AND (symbol_id = ?)");
		statement.setLong(1, volumeReduction);
		statement.setLong(2, orderID);
		statement.setLong(3, symbolIndex);
		statement.execute();
		statement.close();
	}

	@Override
	public void deleteOrder(long orderID, long symbolIndex, long time_ns, long symbolSeqNumber, long packetTimestamp) throws SQLException {
		synchronized (deleteOrderBatch) {
			deleteOrderBatch.setLong(1, orderID);
			deleteOrderBatch.setLong(2, symbolIndex);
			deleteOrderBatch.addBatch();
		}
		batchSize++;
		delete++;
	}

	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns, long symbolSeqNumber, long price, long volume, long packetTimestamp) throws SQLException {
		synchronized (addOrderBatch) {
			addTradeBatch.setLong(1, tradeID);
			addTradeBatch.setLong(2, symbolIndex);
			addTradeBatch.setLong(3, price);
			addTradeBatch.setLong(4, volume);
			addTradeBatch.setLong(5, packetTimestamp);
			addTradeBatch.setLong(6, symbolSeqNumber);
			addTradeBatch.addBatch();
		}
		batchSize++;
	}

	@Override
	public void addStockSummary(long symbolIndex, long time_s, long time_ns, long highPrice, long lowPrice, long openPrice, long closePrice, long totalVolume) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("INSERT INTO stock_summary (symbol_id, high_price, low_price, total_volume, " + "updated_s, updated_ns) " + "VALUES (?, ?, ?, ?, ?, ?)");
		statement.setLong(1, symbolIndex);
		statement.setLong(2, highPrice);
		statement.setLong(3, lowPrice);
		statement.setLong(4, totalVolume);
		statement.setLong(5, time_s);
		statement.setLong(6, time_ns);
		// batchQuery.addBatch(statement.toString().split(":")[1]);
		// batchSize ++;
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID, long symbolIndex, long time_s, long time_ns, long symbolSeqNumber, long price, long volume) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("UPDATE trade SET trade_id = ?, symbol_id = ?, price = ?, volume = ? WHERE (trade_id = ?)");
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, originalTradeID);
		statement.execute();
		statement.close();
	}

	@Override
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber, long referenceTime) throws SQLException {
		// TODO implement method. Not sure how to add a source time reference to
		// the database
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex, long time_s, long time_ns, long symbolSeqNumber) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("DELETE FROM trade WHERE (trade_id = ?)");
		statement.setLong(1, tradeID);
		statement.execute();
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol, long priceScaleCode, long prevClosingPrice, long prevClosingVolume) throws SQLException {
		/*PreparedStatement statement = this.connection.prepareStatement("INSERT INTO symbol (symbol_id, symbol, company_name, price_scale, open_price) VALUES (?,?,?,?,?)");
		statement.setLong(1, symbolIndex);
		statement.setString(2, symbol);
		statement.setString(3, "");
		statement.setLong(4, priceScaleCode);
		statement.setLong(5, prevClosingPrice);*/
	}

	@Override
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns, long symbolSeqNumber, int tradingSession) throws SQLException {
		// TODO implement method. Not sure how to change over a trade session.
		// It will probably involve deleting all orders from that expire within
		// the current trade
		// session.
	}

	public ResultSet getTradesInTimeRangeForSymbol(long symbolID, long start, long end) throws SQLException {
		PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM trade WHERE symbol_id=? and offered_s>=? and offered_s<?");
		ResultSet result;
		try {
			statement.setLong(1, symbolID);
			statement.setLong(2, start);
			statement.setLong(3, end);
			result = statement.executeQuery();
		} finally {
			statement.close();
		}
		return result;
	}
	
	public ResultSet getAllTradesInRecentHistory(long start) throws SQLException{
		PreparedStatement statement = this.connection.prepareStatement(
				"SELECT * FROM trade WHERE offered_s >= ? ORDER BY symbol_id ASC");
		ResultSet result;
		try {
			statement.setLong(1, start);
			result = statement.executeQuery();
		} finally {
			statement.close();
		}
		return result;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void addImbalanceMessage(long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber, long referencePrice)
			throws SQLException {
		// not needed (only for testing purposes)
		
	}

	@Override
	public String getSymbol(long symbolIndex) throws SQLException {
		//TODO (Lucas): implement this method to get the symbol name
		return "Oops, not yet implemented";
	}
}
