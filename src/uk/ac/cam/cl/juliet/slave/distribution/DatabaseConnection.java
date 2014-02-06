package uk.ac.cam.cl.juliet.slave.distribution;

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
	 * Add a new order to the order-book specified by the symbolIndex
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
	 * modifies an existing active order-entry in the database by overwriting
	 * its attributes price, volume, isSell and the time. It has no effect to
	 * the database in case the order does not exists any more in the active
	 * order book.
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

	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction);

	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException;

	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long prize, long volume) throws SQLException;

	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long lighPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException;

	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException;

	/**
	 * Adds a reference time to a stock specified by the symbol index, the
	 * reference time maps to a symbol-sequence-number (other messages refer to
	 * this sequence number to specify a time in seconds)
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

	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException;

	// TODO: This is a misleading name, it should be removeTrade or
	// addTradeCancellation
	// how exactly are we implementing this in the database? -Lucas
	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException;

	/**
	 * Change the current trade session for the symbol specified all open orders
	 * that are not valid for the new trading session have have to be deleted
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
	 * Commits the database changes that have been accumulated
	 * 
	 * @throws SQLException
	 */
	public void commit() throws SQLException;

	/**
	 * Sets the connection to the database
	 * 
	 * @param connection
	 *            The new connection to use.
	 */
	public void setConnection(Connection connection);

}
