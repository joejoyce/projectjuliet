package uk.ac.cam.cl.juliet.slave.distribution;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * This encapsulates the database accesses made by the XDPProcessor and the QueryProcessor
 * on the Raspberry Pis.
 * 
 * @author Lucas Sonnabend
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
	 *            symbol-sequence-number of the message for this symbol index
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
	 *            symbol-sequence-number of the message for this symbol index
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
	 * 		The order ID of the order that is to be reduced
	 * @param symbolIndex
	 * 		The symbol index of the stock for which the order was made
	 * @param time_ns
	 * 		Time stamp from the sender in nanoseconds
	 * @param symbolSeqNumber
	 * 		symbol-sequence-number of the message for this symbol index
	 * @param volumeReduction
	 * 		the amount by which the volume is reduced
	 * @param packetTimestamp
	 * 		The timestamp from the sender of the packet in seconds
	 * @throws SQLException
	 */
	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction, long packetTimestamp) throws SQLException;

	/**
	 * Delete an order from the database order book
	 * 
	 * Note this method will delete an order book entry specified by both orderID and symbolIndex.
	 * 
	 * @param orderID
	 * 			Order ID of the order that is to be deleted
	 * @param symbolIndex
	 * 			symbol index of the stock for which the order was made
	 * @param time_ns
	 * 			time stamp of the sender in nanoseconds
	 * @param symbolSeqNumber
	 * 			sequence number of this message for this symbol index
	 * @param packetTimestamp
	 * 			timestamp of the sender for the packet in seconds
	 * @throws SQLException
	 */
	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException;

	/**
	 * Add a trade to the database trade book
	 * 
	 * @param tradeID
	 * 			unique trade ID of the trade
	 * @param symbolIndex
	 * 			symbol index of the traded stock
	 * @param time_ns
	 * 			time of the sender in nanoseconds
	 * @param symbolSeqNumber
	 * 			sequence number of this message and the symbol index
	 * @param price
	 * 			price at which the stock is traded
	 * @param volume
	 * 			volume that is traded
	 * @param packetTimestamp
	 * 			timestamp of the packet from the sender in seconds
	 * @throws SQLException
	 */
	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException;

	/**
	 * Add a stock summary to database stock summary table.
	 * NOTE: there are no stock summary messages in the sample data stream
	 * 
	 * @param symbolIndex
	 * 		symbol index of the stock.
	 * @param time_s
	 * 		time stamp of the sender for this message in seconds
	 * @param time_ns
	 * 		time stamp of the sender for this message in nanoseconds
	 * @param highPrice
	 * 		highest trading price seen so far
	 * @param lowPrice
	 * 		lowest trading price seen so far
	 * @param openPrice
	 * 		opening price of the stock
	 * @param closePrice
	 * 		closing price of the stock
	 * @param totalVolume
	 * 		total volume traded so far
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
	 * 			original trade ID of the trade that is to be corrected
	 * @param tradeID
	 * 			new trade ID
	 * @param symbolIndex
	 * 			symbol index of the stock that was traded
	 * @param time_s
	 * 			time stamp of the sender (on sending the message) in seconds
	 * @param time_ns
	 * 			time stamp of the sender (on sending the message), only the nanoseconds part
	 * @param symbolSeqNumber
	 * 			sequence number of this message for this symbol index
	 * @param price
	 * 			price at which the stock was traded
	 * @param volume
	 * 			volume that was traded
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
	 *            The ID of the symbol.
	 * @param symbol
	 *            The name of the symbol.
	 * @param priceScaleCode
	 *            The amount which the prices of orders and trades are scaled by.
	 * @param prevClosingPrice
	 *            The last closing price of the symbol.
	 * @param prevClosingVolume
	 *            The last closing volume of the symbol.
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
	 *            The ID of the symbol for which the trading session is changing.
	 * @param time_s
	 *            The seconds part of the time at which the session changed.
	 * @param time_ns
	 *            The nanoseconds part of the time at which the session changed.
	 * @param symbolSeqNumber
	 *            The sequence number of the trading session change message.
	 * @param tradingSession
	 *            Flags which indicate the new session.
	 * @throws SQLException
	 */
	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException;
	
	/**
	 * Queries the database and returns the trades for a specific symbol in a specific time range.
	 * @param symbolID
	 *            The symbol to return trades for.
	 * @param start
	 *            The start time in seconds.
	 * @param end
	 *            The end time in seconds.
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
	 * Gets all trades that happened in the time frame from start up to now 
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
	/**
	 * returns the difference between the lowest offer and the highest bid of 
	 * the stock specified by the symbol index.
	 * If either of them don't exists, it returns 0.
	 * The price scale of the stock has NOT YET been applied to the result
	 * @param symbolIndex
	 * @return
	 * @throws SQLException
	 */
	public long getSpreadOfSymbol(long symbolIndex) throws SQLException;
	
	/**
	 * Return the time in nanoseconds that the last commit took.
	 * @return
	 */
	public long getLastCommitNS();
	
	/**
	 * Fetches the limit best offers for a given symbolId
	 * @param symbolID
	 * @param limit
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getBestOffersForStock(long symbolID, int limit) throws SQLException;

	/**
	 * Adds a runnable which will be executed whenever a batch statement is about to be executed.
	 * @param r The runnable to run. 
	 */
	public void addBatchQueryExecuteStartCallback(Runnable r);
	
	/**
	 * Adds a runnable which will be executed whenever a batch statement has been executed.
	 * @param r The runnable to run.
	 */
	public void addBatchQueryExecuteEndCallback(Runnable r);
	
	
	public void maybeEmergencyBatch();

	public void unlockTables() throws SQLException;
}
