package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.cam.cl.juliet.common.Debug;

public class DatabaseConnectionUnit implements DatabaseConnection {
	private class OrderVolumeReduction {
		long orderID;
		long symbolID;
		long amount;
		long time;
		long sequenceNumber;

		OrderVolumeReduction(long orderID, long symbolID, long amount,
				long time, long sequenceNumber) {
			this.orderID = orderID;
			this.symbolID = symbolID;
			this.amount = amount;
			this.time = time;
			this.sequenceNumber = sequenceNumber;
		}

		private boolean tryToApply() {
			try {
				PreparedStatement statement = connection.prepareStatement("SELECT updated_s, updated_seq_num FROM order_book WHERE order_id = ? AND symbol_id = ?");
				statement.setLong(1, orderID);
				statement.setLong(2, symbolID);
				ResultSet results = statement.executeQuery();
				statement.close();
				
				if (!results.isBeforeFirst())
					return false;
				
				results.next();
				long updated_s = results.getLong("updated_s");
				long updated_seq_num = results.getLong("updated_seq_num");
				
				if (updated_s<time || (updated_s == time && updated_seq_num < sequenceNumber)) {
					statement = connection
							.prepareStatement("UPDATE order_book SET volume = volume - ? WHERE (order_id = ?) AND (symbol_id = ?)");
					statement.setLong(1, amount);
					statement.setLong(2, orderID);
					statement.setLong(3, symbolID);
					statement.execute();
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		}
	}

	private Connection connection;
	private PreparedStatement addOrderBatch;
	private PreparedStatement addTradeBatch;
	private PreparedStatement deleteOrderBatch;
	private PreparedStatement modifyOrderBatch;
	private ArrayList<OrderVolumeReduction> volumeReductions = new ArrayList<OrderVolumeReduction>();
	private final static ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	private int batchSize = 0;
	private int delete = 0;
	private int add = 0;

	private long lastCommitNs = -1;

	public DatabaseConnectionUnit(Connection c) throws SQLException {
		this.connection = c;
		this.addOrderBatch = connection
				.prepareStatement("CALL addOrder(?, ?, ?, ?, ?, ?, ?, ?, ?)");
		this.addTradeBatch = connection
				.prepareStatement("CALL addTrade(?, ?, ?, ?, ?, ?, ?, ?)");
		// this.addTradeBatch =
		// connection.prepareStatement("INSERT INTO trade VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0)");
		// this.deleteOrderBatch =
		// connection.prepareStatement("UPDATE order_book SET is_deleted=1 WHERE (order_id = ?) AND (symbol_id = ?)");
		// this.deleteOrderBatch =
		// connection.prepareStatement("DELETE FROM order_book WHERE (order_id = ?) AND (symbol_id = ?)");
		this.deleteOrderBatch = connection
				.prepareStatement("CALL deleteOrder(?, ?, ?, ?)");
		// this.modifyOrderBatch =
		// connection.prepareStatement("UPDATE order_book SET price = ?, volume = ?, updated_s = ?, updated_seq_num = ? WHERE (order_id = ?) AND (symbol_id = ?)");
		this.modifyOrderBatch = connection
				.prepareStatement("CALL modifyOrder(?, ?, ?, ?, ?, ?)");

		// Negative as not yet done

		final Runnable executeBatch = new Runnable() {
			public void run() {
				try {
					Debug.println(Debug.INFO, "Total batch size: " + batchSize);
					long start = System.nanoTime();

					Debug.println(Debug.INFO,
							"About to execute addOrder batch: " + add);
					add = 0;
					long then = start;
					synchronized (addOrderBatch) {
						addOrderBatch.executeBatch();
						addOrderBatch.clearBatch();
					}
					double diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					Debug.println(Debug.INFO, "Taken: " + diff);

					Debug.println(Debug.INFO, "About to execute addTrade batch");
					then = System.nanoTime();
					synchronized (addTradeBatch) {
						addTradeBatch.executeBatch();
						addTradeBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					Debug.println(Debug.INFO, "Taken: " + diff);

					Debug.println(Debug.INFO,
							"About to execute deleteOrder batch: " + delete);
					delete = 0;
					then = System.nanoTime();
					synchronized (deleteOrderBatch) {
						deleteOrderBatch.executeBatch();
						deleteOrderBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					Debug.println(Debug.INFO, "Taken: " + diff);

					Debug.println(Debug.INFO,
							"About to execute modifyOrderBatch batch");
					then = System.nanoTime();
					synchronized (modifyOrderBatch) {
						modifyOrderBatch.executeBatch();
						modifyOrderBatch.clearBatch();
					}
					diff = Math.abs(System.nanoTime() - then);
					diff /= 1000000;
					Debug.println(Debug.INFO, "Taken: " + diff);

					synchronized (volumeReductions) {
						for (int i = 0; i < volumeReductions.size();) {
							if (volumeReductions.get(i).tryToApply()) {
								volumeReductions.remove(i);
							} else
								i++;
						}
					}

					batchSize = 0;

					long totalTaken = Math.abs(System.nanoTime() - start);
					lastCommitNs = totalTaken;
					totalTaken /= 1000000;
					Debug.println(Debug.INFO, "Total time taken: " + totalTaken);
					Debug.println(Debug.INFO,
							"-------------------------------------");
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};
		scheduler.scheduleAtFixedRate(executeBatch, 5, 1, TimeUnit.SECONDS);
	}

	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			int tradeSession, long packetTimestamp) throws SQLException {
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
		add++;
	}

	@Override
	public void modifyOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) throws SQLException {
		synchronized (modifyOrderBatch) {
			modifyOrderBatch.setLong(1, orderID);
			modifyOrderBatch.setLong(2, symbolIndex);
			modifyOrderBatch.setLong(3, price);
			modifyOrderBatch.setLong(4, volume);
			modifyOrderBatch.setLong(5, packetTimestamp);
			modifyOrderBatch.setLong(6, symbolSeqNumber);
			modifyOrderBatch.addBatch();
		}
		batchSize++;
	}

	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction, long packetTimestamp)
			throws SQLException {
		synchronized (volumeReductions) {
			this.volumeReductions.add(new OrderVolumeReduction(orderID,
					symbolIndex, volumeReduction, packetTimestamp,
					symbolSeqNumber));
		}
	}

	@Override
	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException {
		synchronized (deleteOrderBatch) {
			deleteOrderBatch.setLong(1, orderID);
			deleteOrderBatch.setLong(2, symbolIndex);
			deleteOrderBatch.setLong(3, packetTimestamp);
			deleteOrderBatch.setLong(4, symbolSeqNumber);
			deleteOrderBatch.addBatch();
		}
		batchSize++;
		delete++;
	}

	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException {
		synchronized (addTradeBatch) {
			addTradeBatch.setLong(1, tradeID);
			addTradeBatch.setLong(2, symbolIndex);
			addTradeBatch.setLong(3, price);
			addTradeBatch.setLong(4, volume);
			addTradeBatch.setLong(5, packetTimestamp);
			addTradeBatch.setLong(6, symbolSeqNumber);
			addTradeBatch.setLong(7, packetTimestamp);
			addTradeBatch.setLong(8, symbolSeqNumber);
			addTradeBatch.addBatch();
		}
		batchSize++;
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
		// batchQuery.addBatch(statement.toString().split(":")[1]);
		// batchSize ++;
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("UPDATE trade SET trade_id = ?, symbol_id = ?, price = ?, volume = ? WHERE (trade_id = ?)");
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, price);
		statement.setLong(4, volume);
		statement.setLong(5, originalTradeID);
		// statement.execute();
		// statement.close();
	}

	@Override
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException {
		// TODO implement method. Not sure how to add a source time reference to
		// the database
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("CALL deleteTrade(?, ?, ?, ?)");
		statement.setLong(1, tradeID);
		statement.setLong(2, symbolIndex);
		statement.setLong(3, time_s);
		statement.setLong(4, symbolSeqNumber);
		statement.execute();
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException {
		/*
		 * PreparedStatement statement = this.connection.prepareStatement(
		 * "INSERT INTO symbol (symbol_id, symbol, company_name, price_scale, open_price) VALUES (?,?,?,?,?)"
		 * ); statement.setLong(1, symbolIndex); statement.setString(2, symbol);
		 * statement.setString(3, ""); statement.setLong(4, priceScaleCode);
		 * statement.setLong(5, prevClosingPrice);
		 */
	}

	@Override
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException {
		// TODO implement method. Not sure how to change over a trade session.
		// It will probably involve deleting all orders from that expire within
		// the current trade
		// session.
	}

	public ResultSet getTradesInTimeRangeForSymbol(long symbolID, long start,
			long end) throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("SELECT * FROM trade WHERE symbol_id=? AND offered_s>=? AND offered_s<? AND added=1 AND deleted=0");
		ResultSet result;
		statement.setLong(1, symbolID);
		statement.setLong(2, start);
		statement.setLong(3, end);
		result = statement.executeQuery();
		return result;
	}

	public ResultSet getAllTradesInRecentHistory(long start)
			throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("SELECT * FROM trade WHERE offered_s >= ? AND added=1 AND deleted=0 ORDER BY symbol_id");
		ResultSet result;
		statement.setLong(1, start);
		result = statement.executeQuery();
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
	public ResultSet getSymbolAndPriceScale(long symbolIndex)
			throws SQLException {
		PreparedStatement statement = this.connection
				.prepareStatement("SELECT symbol,price_scale FROM symbol WHERE symbol_id = ?");
		ResultSet result;
		statement.setLong(1, symbolIndex);
		result = statement.executeQuery();
		if (!result.isBeforeFirst())
			return null;
		else
			return result;
	}

	@Override
	public long getSpreadOfSymbol(long symbolIndex) throws SQLException {
		// get the lowest offer and highest bid for a stock:
		PreparedStatement statement = this.connection
				.prepareStatement("(SELECT price FROM order_book WHERE symbol_id = ? "
						+ "AND is_ask = 1 AND added=1 AND deleted=0 ORDER BY price LIMIT 1)"
						+ " UNION (SELECT price FROM order_book WHERE symbol_id = ? "
						+ "AND is_ask = 0 AND AND added=1 deleted=0 ORDER BY price DESC LIMIT 1)");

		ResultSet result;
		statement.setLong(1, symbolIndex);
		statement.setLong(2, symbolIndex);
		result = statement.executeQuery();

		long lowestOffer = 0;
		long highestBid = 0;
		if (result.next()) {
			lowestOffer = result.getLong(1);
		}
		if (result.next()) {
			highestBid = result.getLong(1);
		}

		result.close();

		if (highestBid == 0 || lowestOffer == 0)
			return 0;
		else
			return lowestOffer - highestBid;
	}

	public long getLastCommitNS() {
		return lastCommitNs;
	}
}
