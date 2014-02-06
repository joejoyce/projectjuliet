package uk.ac.cam.cl.juliet.test.messagedecoding;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;

public class MockDatabaseConnection implements DatabaseConnection{
	private HashMap<Long, MessageStatisticsDatum> sMessageStatistics; 
	private PrintWriter symbolMappingOut;
	private long singleStockSymbolIndex;
	private List<String> singleStockMessageLog;
	private String mDatapath;
	
	private class MessageStatisticsDatum {
		public int noMsgsAddOrder;
		public int noMsgsModifyOrder;
		public int noMsgsExecuteOrder;
		public int noMsgsTimeReference;
		public int noMsgsSymbolMapping;
		public int noMsgsTradeSessionChange;
		public int noMsgsDeleteOrder;
		public int noMsgsTradeCancel;
		public int noMsgsTradeCorrection;
		public int noMsgsStockSummary;
		
		public MessageStatisticsDatum() {
			noMsgsAddOrder = 0;
			noMsgsModifyOrder = 0;
			noMsgsExecuteOrder = 0;
			noMsgsTimeReference = 0;
			noMsgsSymbolMapping = 0;
			noMsgsTradeSessionChange = 0;
			noMsgsDeleteOrder = 0;
			noMsgsTradeCancel = 0;
			noMsgsTradeCorrection = 0;
			noMsgsStockSummary = 0;
		}
		
	}
	/**
	 * This will create a new MockDatabaseConnection that can be used for testing
	 * the packet processing and message decoding. It will create csv for
	 * symbol-index-mappings, an overview of how many and what kind of messages
	 * arrived for each symbol and a detailed list of every message for one symbol.
	 * @param datapath 
	 * 			specifies the path, at which the created .csv files are stored,
	 * 			it must NOT end with a slash
	 * @param symbolIndex
	 * 			the symbolIndex of the stock of which a detailed log is created
	 * @throws IOException
	 */
	public MockDatabaseConnection(String datapath, long symbolIndex) throws IOException {
		mDatapath = datapath;
		singleStockSymbolIndex = symbolIndex;
		String symbolMappingCSV = datapath +"/symbolMapping.csv"; 
		FileWriter fw;
		fw = new FileWriter(symbolMappingCSV);
		symbolMappingOut = new PrintWriter(fw);
		symbolMappingOut.print("symbol,symbolIndex,priceScaleCode,"+
				"prevClosingPrice,prevClosingVolume\n");
		symbolMappingOut.flush();
		
		sMessageStatistics = new HashMap<Long, MessageStatisticsDatum>();
		singleStockMessageLog = new LinkedList<String>();
	}
	
	/**
	 * This has to be called to close a PrintWriter stream the database is using
	 * @throws IOException
	 */
	public void closeMockDatabase() throws IOException {
		symbolMappingOut.close();
	}
	
	/**
	 * Call this method to write the accumulated information of the stock to a
	 * file.
	 * @throws IOException
	 */
	public void writeSingleStockeLog() throws IOException{
		String file = mDatapath +"/singleStockMessageLog.csv";
		FileWriter fw = new FileWriter(file);
		PrintWriter out = new PrintWriter(fw);
		out.print("MessageType,OrderID,TradeID,time_s,time_ns,symbolSequenceNumber,"+
				"price,volume,isSell,TradeSession,highprice,lowprice\n");
		for(String s : singleStockMessageLog) {
			out.println(s);
		}
		out.close();
	}
	/**
	 * This method writes the accumulated statistics containing how many messages
	 * where seen per stock to a file.
	 * @throws IOException
	 */
	public void writeStatisticsToFile() throws IOException{
		String symbolMappingCSV = mDatapath +"/messageStats.csv"; 
		FileWriter fw = new FileWriter(symbolMappingCSV);
		PrintWriter out = new PrintWriter(fw);
		out.print("symbolIndex,symbolIndexMappings,addOrder,modifyOrder,"+
				"executeOrder,deleteOrder,tradeSessionChanges,tradeCancel,"+
				"tradeCorrections,summaries,TimeSourceReference\n");
		out.flush();
		
		for(Entry<Long, MessageStatisticsDatum> e : sMessageStatistics.entrySet()) {
			out.print(e.getKey()+",");
			MessageStatisticsDatum msd = e.getValue();
			out.print(msd.noMsgsSymbolMapping+","+msd.noMsgsAddOrder+","+
					msd.noMsgsModifyOrder+","+msd.noMsgsExecuteOrder+","+
					msd.noMsgsDeleteOrder+","+msd.noMsgsTradeSessionChange+","+
					msd.noMsgsTradeCancel+","+msd.noMsgsTradeCorrection+","+
					msd.noMsgsStockSummary+","+msd.noMsgsTimeReference+"\n");
			out.flush();
		}
		out.close();
	}
	
	@Override
	public void addOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume,
			boolean isSell, int tradeSession, long timestamp) throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsAddOrder++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsAddOrder++;
		}	
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("AddOrder,"+orderID+","+"-"+","+"-"+","+time_ns+","+
					symbolSeqNumber+","+price+","+volume+","+isSell+","+tradeSession);
	}

	@Override
	public void modifyOrder(long orderID, long symbolIndex,
			long time_ns, long symbolSeqNumber, long price,
			long volume, boolean isSell, long timestamp) throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsModifyOrder++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsModifyOrder++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("modifyOrder,"+orderID+","+"-"+","+"-"+","+time_ns+","+
					symbolSeqNumber+","+price+","+volume+","+isSell);
		
	}

	@Override
	public void reduceOrderVolume(long orderID, long symbolIndex,
			long time_ns, long symbolSeqNumber, long volumeReduction) {
		// part of the order execution message
		
	}

	@Override
	public void deleteOrder(long orderID, long symbolIndex,
			long time_ns, long symbolSeqNumber,  long timestamp) throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsDeleteOrder++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsDeleteOrder++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("DeleteOrder,"+orderID+","+"-"+","+"-"+","+time_ns+","+
					symbolSeqNumber);
	}

	@Override
	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume)
			throws SQLException {
		//part of the orderExecution message
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsExecuteOrder++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsExecuteOrder++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("OrderExecution,"+"-"+","+tradeID+","+"-"+","+time_ns+","+
					symbolSeqNumber+","+price+","+volume);
		
	}

	@Override
	public void addStockSummary(long symbolIndex, long time_s,
			long time_ns, long highPrice, long lowPrice,
			long openPrice, long closePrice, long totalVolume)
			throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsStockSummary++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsStockSummary++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("addStockSummary,"+"-"+","+"-"+","+time_s+","+time_ns+","+
					"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+
					highPrice+","+lowPrice);
	}

	@Override
	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, long price, long volume)
			throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsTradeCorrection++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsTradeCorrection++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("correctTrade,"+"-"+","+originalTradeID+","+time_s+","+time_ns+","+
					symbolSeqNumber+","+price+","+volume+",");
	}

	@Override
	public void addSourceTimeReference(long symbolIndex,
			long symbolSeqNumber, long referenceTime)
			throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsTimeReference++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsTimeReference++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("AddSourceTimeReference,"+"-"+","+"-"+","+referenceTime+","+"-"+","+
					symbolSeqNumber);
	}

	@Override
	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice,
			long prevClosingVolume) throws SQLException {
		
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsSymbolMapping++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsSymbolMapping++;
		}	
		
		// write into a csv file:
		symbolMappingOut.print(symbol + ","+symbolIndex+","+priceScaleCode+","+
				prevClosingPrice+","+prevClosingVolume+"\n");
		symbolMappingOut.flush();
		
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("SymbolMapping");
	}

	@Override
	public void cancelTrade(long tradeID, long symbolIndex,
			long time_s, long time_ns, long symbolSeqNumber)
			throws SQLException {
		
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsTradeCancel++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsTradeCancel++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("CancelTrade,"+"-"+","+tradeID+","+"-"+","+time_ns+","+
					symbolSeqNumber);
	}

	@Override
	public void changeTradeSession(long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber, int tradingSession)
			throws SQLException {
		if(!sMessageStatistics.containsKey(symbolIndex)) {
			MessageStatisticsDatum newDatum = new MessageStatisticsDatum();
			newDatum.noMsgsTradeSessionChange++;
			sMessageStatistics.put(symbolIndex, newDatum);
		} else {
			sMessageStatistics.get(symbolIndex).noMsgsTradeSessionChange++;
		}
		if(symbolIndex == singleStockSymbolIndex)
			singleStockMessageLog.add("changeTradeSession,"+"-"+","+"-"+","+time_s+","+time_ns+","+
					symbolSeqNumber+","+"-"+","+"-"+","+"-"+","+tradingSession);
	}

	@Override
	public void commit() throws SQLException {
		// not needed for this test database connection
		
	}
	
	public void setConnection(Connection connection) {
		// not needed
	}
}
