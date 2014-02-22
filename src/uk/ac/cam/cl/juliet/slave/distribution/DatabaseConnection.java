package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * This encapsulates the database accesses made by the XDPProcessor. It can be
 * mocked to do packet decoding testing.
 * 
 * @author lucas
 * 
 */
public interface DatabaseConnection {
	/**
	 * Add a new order to the order book specified by the symbolIndex
	 * 
	 * @param orderID
	 *            unique orderID of the new order
	 * @param symbolIndex
	 *            symbolIndex of the stock, determining the order book
	 * @param time_ns
	 *            timestamp from the sender in nanoseconds
	 * @param symbolSeqNumber
	 *            symbol-sequence-number refers to an EPOCH time stamp in
	 *            seconds for this stock
	 * @param price
	 *            price of the order
	 * @param volume
	 *            volume of the order
	 * @param isSell
	 *            true if it is sell/ask order and false if it is buy/bid
	 * @param tradeSession
	 *            trade sessions for which the order is valid
	 * @throws SQLException
	 */
	public void addOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			int tradeSession, long packetTimestamp) throws SQLException;

	/**
	 * Modify an existing active order book database entry
	 * 
	 * This method overwrites the order book entry's attributes of price, volume, isSell and time.
	 * It has no effect to the database if the order no longer exists in the order book.
	 * 
	 * @param orderID
	 *            order ID of the order to be modified
	 * @param symbolIndex
	 *            defines the stock and the order-book in which the order
	 *            resides
	 * @param time_ns
	 *            timestamp from the sender in nanoseconds
	 * @param symbolSeqNumber
	 *            symbol-sequence-number refers to an EPOCH time stamp in
	 *            seconds for this stock
	 * @param price
	 *            potential new price of the order
	 * @param volume
	 *            potential new volume of the order
	 * @param isSell
	 *            true if it is sell/ask order and false if it is buy/bid
	 * @throws SQLException
	 */
	public void modifyOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) throws SQLException;

	
	/**
	 * Reduce the volume of an order's order book database entry
	 * 
	 * @param orderID
	 * @param symbolIndex
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param volumeReduction
	 * @throws SQLException
	 */
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction) throws SQLException;

	/**
	 * Delete an order from the database order book
	 * 
	 * Note this method will delete an order book entry specified by both orderID and symbolIndex.
	 * 
	 * @param orderID
	 * @param symbolIndex
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param packetTimestamp
	 * @throws SQLException
	 */
	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException;

	/**
	 * Add a trade to the database trade book
	 * 
	 * @param tradeID
	 * @param symbolIndex
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param price
	 * @param volume
	 * @param packetTimestamp
	 * @throws SQLException
	 */
	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException;

	/**
	 * Add a stock summary to database stock summary table
	 * 
	 * @param symbolIndex
	 * @param time_s
	 * @param time_ns
	 * @param highPrice
	 * @param lowPrice
	 * @param openPrice
	 * @param closePrice
	 * @param totalVolume
	 * @throws SQLException
	 */
	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long highPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException;

	/**
	 * Correct a trade entry in the database
	 * 
	 * This method will alter an existing trade entry specified by originalTradeID.
	 * 
	 * @param originalTradeID
	 * @param tradeID
	 * @param symbolIndex
	 * @param time_s
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param price
	 * @param volume
	 * @throws SQLException
	 */
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException;

	/**
	 * Add a reference time to a stock specified by the symbol index
	 * 
	 * The reference time maps to a symbol-sequence-number (other messages refer to this sequence
	 * number to specify a time in seconds)
	 * 
	 * @param symbolIndex
	 *            specifies the stock the time reference is connected to
	 * @param symbolSeqNumber
	 *            sequence number of a sequence that is unique for every symbol
	 *            index
	 * @param referenceTime
	 *            time in seconds (EPOCH) the sequence number maps to
	 * @throws SQLException
	 */
	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException;

	/**
	 * Add a symbol mapping entry to the database's symbol table
	 * 
	 * @param symbolIndex
	 * @param symbol
	 * @param priceScaleCode
	 * @param prevClosingPrice
	 * @param prevClosingVolume
	 * @throws SQLException
	 */
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException;

	/**
	 * Cancel a trade in the database
	 * 
	 * This method will delete a trade record from the database to reflect the cancellation of the
	 * trade.
	 * 
	 * @param tradeID
	 *            the trade ID of the trade to cancel
	 * @param symbolIndex
	 *            the stock to which the trade relates
	 * @param time_s
	 *            timestamp from the sender in seconds
	 * @param time_ns
	 *            timestamp from the sender in nanoseconds
	 * @param symbolSeqNumber
	 *            symbol-sequence-number of the stock - refers to an epoch timestamp in seconds for
	 *            this stock
	 * @throws SQLException
	 */
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException;

	/**
	 * Change the current trade session for the symbol specified
	 * 
	 * All open orders that are not valid for the new trading session have to be deleted.
	 * 
	 * @param symbolIndex
	 * @param time_s
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param tradingSession
	 * @throws SQLException
	 */
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException;
	
	/**
	 * Queries the database and returns the trades for a specific symbol in a specific time range.
	 * @param symbolID
	 * @param start
	 * @param end
	 * @return The trades for the specified symbol in the specified time range.
	 */
	public ResultSet getTradesInTimeRangeForSymbol(long symbolID, long start, long end) throws SQLException;

	/**
	 * Insert information about a new imbalance message into the database.
	 * Right now this is only used for collecting information during a test
	 * (see test.messageDecoding)
	 * @param symbolIndex
	 * @param time_s
	 * @param time_ns
	 * @param symbolSeqNumber
	 * @param referencePrice
	 * @throws SQLException
	 */
	public void addImbalanceMessage(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, long referencePrice) throws SQLException;
	
	/**
	 * gets all trades that happened in the time frame from start up to now 
	 * ordered by the symbol ID
	 * @param start		lower limit of time frame
	 * @return
	 */
	public ResultSet getAllTradesInRecentHistory(long start) throws SQLException;
	
	/**
	 * Returns the stock symbol that is associated with the symbolIndex
	 * @param symbolIndex
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getSymbolAndPriceScale(long symbolIndex) throws SQLException;
	/**
	 * Set the connection to the database
	 * 
	 * @param connection
	 *            The new connection to use.
	 */
	public void setConnection(Connection connection);
	
	public long getSpreadOfSymbol(long symbolIndex) throws SQLException;
	
	/**
	 * Return the time in nanoseconds that the last commit took.
	 * @return
	 */
	public long getLastCommitNS();
}
