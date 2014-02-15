package uk.ac.cam.cl.juliet.slave.queryprocessing;

import java.sql.ResultSet;
import java.sql.SQLException;

import uk.ac.cam.cl.juliet.common.CandlestickRequest;
import uk.ac.cam.cl.juliet.common.CandlestickResponse;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.QueryResponse;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;

/**
 * Handles all query packets sent from the master to the slave.
 * 
 * @author Dylan McDermott
 * 
 */
public class QueryProcessorUnit implements QueryProcessor {

	private DatabaseConnection connection;

	public QueryProcessorUnit(DatabaseConnection connection) {
		this.connection = connection;
	}

	@Override
	public QueryResponse runQuery(QueryPacket p) {
		if (p instanceof CandlestickRequest)
			return handleCandlestickRequest((CandlestickRequest) p);
		else {
			// Unknown query
			return new QueryResponse(p.getPacketId(), false);
		}
	}

	private QueryResponse handleCandlestickRequest(CandlestickRequest p) {
		try {
			ResultSet results = connection.getTradesInTimeRangeForSymbol(
					p.getSymbolId(),
					p.getStart(),
					p.getStart() + p.getResolution());
			long open = 0;
			long earliestTradeSeconds = 0;
			long earliestTradeSeq = 0;
			long close = 0;
			long latestTradeSeconds = 0;
			long latestTradeSeq = 0;
			long high = 0;
			long low = 0;
			long volume = 0;

			while (results.next()) {

				long s = results.getLong("offered_s");
				long seq = results.getLong("offered_seq_num");
				long price = results.getLong("price");
				if (earliestTradeSeconds == 0
						|| earliestTradeSeconds > s
						|| (earliestTradeSeconds == s && earliestTradeSeq > seq)) {
					earliestTradeSeconds = s;
					earliestTradeSeq = seq;
					open = price;
				}
				if (latestTradeSeconds == 0
						|| latestTradeSeconds < s
						|| (latestTradeSeconds == s && latestTradeSeq < seq)) {
					latestTradeSeconds = s;
					latestTradeSeq = seq;
					close = price;
				}

				if (high == 0 || price > high)
					high = price;
				if (low == 0 || price < low)
					low = price;

				volume = results.getLong("volume");
			}

			return new CandlestickResponse(p.getPacketId(), p.getStart(), open,
					close, high, low, volume);
		} catch (SQLException e) {
			return new QueryResponse(p.getPacketId(), false); // Fail query
		}
	}

}
