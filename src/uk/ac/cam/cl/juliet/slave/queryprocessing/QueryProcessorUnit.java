package uk.ac.cam.cl.juliet.slave.queryprocessing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import uk.ac.cam.cl.juliet.common.CandlestickRequest;
import uk.ac.cam.cl.juliet.common.CandlestickResponse;
import uk.ac.cam.cl.juliet.common.MovingAverageRequest;
import uk.ac.cam.cl.juliet.common.MovingAverageResponse;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.QueryResponse;
import uk.ac.cam.cl.juliet.common.SpikeDetectionRequest;
import uk.ac.cam.cl.juliet.common.SpikeDetectionResponse;
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
		if (p instanceof MovingAverageRequest)
			return handleMovingAverageRequest((MovingAverageRequest) p);
		if (p instanceof SpikeDetectionRequest)
			return handleSpikeDetecionRequest((SpikeDetectionRequest) p);
		else {
			// Unknown query
			return new QueryResponse(p.getPacketId(), false);
		}
	}

	private QueryResponse handleSpikeDetecionRequest(SpikeDetectionRequest p) {
		SpikeDetectionResponse response = new SpikeDetectionResponse(
				p.getPacketId(), true);
		try {
			//get data from database
			ResultSet queryResults = connection.getAllTradesInRecentHistory(
					p.getStartTimeAverage());
			Boolean thereAreMoreQueryResults = queryResults.next();
			while(thereAreMoreQueryResults) {
				//get all trades for one stock
				ArrayList<Trade> tradeList = new ArrayList<Trade>();
				long currentSymbol = queryResults.getLong("symbol_id");
				tradeList.add(new Trade(queryResults.getLong("offered_s"),
						queryResults.getLong("offered_ns"), queryResults.getLong("price")));
				thereAreMoreQueryResults = queryResults.next();
				while(thereAreMoreQueryResults && 
						queryResults.getLong("symbol_id") == currentSymbol) {
						tradeList.add(new Trade(queryResults.getLong("offered_s"),
								queryResults.getLong("offered_ns"), queryResults.getLong("price")));
						thereAreMoreQueryResults = queryResults.next();
				}
				//detect spike for this stock and add to response packet
				if(!tradeList.isEmpty()) {
					detectSpike(tradeList, response, p.getStartTimeSpikes(), 
							p.getLimit(), currentSymbol);
				}
			}
			
		} catch (SQLException e) {
			return new QueryResponse(p.getPacketId(), false); // query failed
		}
		
		return response;
	}
	/**
	 * 
	 * @param tradeList the list of trades in which a spike shall be detected
	 * @param response 
	 * @precondition the size of the tradeList must not be 0!
	 */
	private void detectSpike(ArrayList<Trade> tradeList, 
			SpikeDetectionResponse response, long pStartTime, float limit,
			long symbolIndex) throws SQLException{
		//compute average price
		int counter = 0;
		int spikeDetectionPointer = -1;
		long averagePrice = 0;
		for(Trade trade : tradeList) {
			averagePrice += trade.price;
			//test whether the trade you looked at is within the time frame for spike detection
			if(trade.seconds >= pStartTime && spikeDetectionPointer < 0)
				spikeDetectionPointer = counter;
			counter++;
		}
		//look for a spike
		while(spikeDetectionPointer < counter) {
			Trade consideredTrade = tradeList.get(spikeDetectionPointer);
			double price = consideredTrade.price*counter;
			double highBoundary = (double) averagePrice * (1.0+limit);
			double lowBoundary = (double) averagePrice * (1.0 - limit);
			if( price <= lowBoundary || price >= highBoundary){
				//we have a spike
				String symbol = this.connection.getSymbol(symbolIndex);
				response.addSpike(symbol, consideredTrade.seconds);
			}
		}
	}

	private QueryResponse handleMovingAverageRequest(MovingAverageRequest p) {
		try {
			ResultSet results = connection
					.getTradesInTimeRangeForSymbol(p.getSymbolId(),
							p.getStart(), p.getStart() + p.getLength());

			ArrayList<Trade> resultList = new ArrayList<Trade>();

			while (results.next())
				resultList.add(new Trade(results.getLong("offered_s"), results
						.getLong("offered_ns"), results.getLong("price")));

			Collections.sort(resultList);

			ArrayList<Long> times = new ArrayList<Long>();
			ArrayList<Double> averages = new ArrayList<Double>();

			long lastAverageTime = p.getStart() + p.getLength()
					- p.getSecondsPerAverage();
			for (int i = 0; i < resultList.size()
					&& resultList.get(i).seconds <= lastAverageTime; i++) {
				long total = 0;
				long start = resultList.get(i).seconds;
				long end = start + p.getSecondsPerAverage();
				int count = 0;
				for (int j = i; j < resultList.size()
						&& resultList.get(j).seconds <= end; j++, count++)
					total += resultList.get(j).price;
				if (count > 0) {
					times.add(start);
					averages.add((double) total / count);
				}
			}

			long[] timesArray = new long[times.size()];
			for (int i = 0; i < times.size(); i++)
				timesArray[i] = times.get(i);
			double[] averagesArray = new double[averages.size()];
			for (int i = 0; i < averages.size(); i++)
				averagesArray[i] = averages.get(i);

			return new MovingAverageResponse(p.getPacketId(), timesArray,
					averagesArray);

		} catch (SQLException e) {
			return new QueryResponse(p.getPacketId(), false); // Fail query
		}
	}

	private QueryResponse handleCandlestickRequest(CandlestickRequest p) {
		try {
			ResultSet results = connection.getTradesInTimeRangeForSymbol(
					p.getSymbolId(), p.getStart(),
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
				if (latestTradeSeconds == 0 || latestTradeSeconds < s
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
	
	private class Trade implements Comparable<Trade> {
		public long seconds;
		public long nanoseconds;
		public long price;

		public Trade(long seconds, long nanoseconds, long price) {
			this.seconds = seconds;
			this.nanoseconds = nanoseconds;
			this.price = price;
		}

		@Override
		public int compareTo(Trade o) {
			if (this.seconds < o.seconds)
				return -1;
			else if (this.seconds > o.seconds)
				return 1;
			else if (this.nanoseconds < o.nanoseconds)
				return -1;
			else if (this.nanoseconds > o.nanoseconds)
				return 1;
			else
				return 0;
		}
	}

}
