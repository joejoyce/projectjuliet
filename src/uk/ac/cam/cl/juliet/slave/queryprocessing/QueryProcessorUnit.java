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
					p.getStart() + p.getNumberOfCandlesticks()
							* p.getResolution());
			long[] open = new long[p.getNumberOfCandlesticks()];
			long[] earliestTradeSeconds = new long[p.getNumberOfCandlesticks()];
			long[] earliestTradeSeq = new long[p.getNumberOfCandlesticks()];
			long[] close = new long[p.getNumberOfCandlesticks()];
			long[] latestTradeSeconds = new long[p.getNumberOfCandlesticks()];
			long[] latestTradeSeq = new long[p.getNumberOfCandlesticks()];
			long[] high = new long[p.getNumberOfCandlesticks()];
			long[] low = new long[p.getNumberOfCandlesticks()];
			long[] volume = new long[p.getNumberOfCandlesticks()];

			while (results.next()) {
				int index = (int) ((results.getLong("offered_s") - p.getStart()) / p
						.getNumberOfCandlesticks());

				long s = results.getLong("offered_s");
				long seq = results.getLong("offered_seq_num");
				long price = results.getLong("price");
				if (earliestTradeSeconds[index] == 0
						|| earliestTradeSeconds[index] > s
						|| (earliestTradeSeconds[index] == s && earliestTradeSeq[index] > seq)) {
					earliestTradeSeconds[index] = s;
					earliestTradeSeq[index] = seq;
					open[index] = price;
				}
				if (latestTradeSeconds[index] == 0
						|| latestTradeSeconds[index] < s
						|| (latestTradeSeconds[index] == s && latestTradeSeq[index] < seq)) {
					latestTradeSeconds[index] = s;
					latestTradeSeq[index] = seq;
					close[index] = price;
				}

				if (high[index] == 0 || price > high[index])
					high[index] = price;
				if (low[index] == 0 || price < low[index])
					low[index] = price;

				volume[index] = results.getLong("volume");
			}

			return new CandlestickResponse(p.getPacketId(), p.getStart(), open,
					close, high, low, volume);
		} catch (SQLException e) {
			return new QueryResponse(p.getPacketId(), false); // Fail query
		}
	}

}
