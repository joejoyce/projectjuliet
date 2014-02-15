package uk.ac.cam.cl.juliet.test.messagedecoding;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;


//TestStringBuilder
class TSB {
	StringBuilder str = new StringBuilder();
	
	public void add(int i) {
		str.append(",");
		str.append(i);
	}
	public void add(long l) {
		str.append(",");
		str.append(l);
	}
	public void add(boolean b) {
		str.append(",");
		str.append(b);
	}
	public String getStr() {
		return str.toString();
	}
}


class TestDatabase implements DatabaseConnection {

	private LinkedList<String> ll = new LinkedList<String>();
	private int numFailed = 0;
	private boolean checking = false;
	private long seconds = 0;
	
	public TestDatabase() {
		seconds = System.currentTimeMillis() / 1000L;
	}
	
	public long getSeconds() {
		return seconds;
	}
	
	private byte[] bytes = new byte[1500];
	int bdex = 16;

	private void pushByte(int b ) {
		bytes[bdex++] = (byte)( 0xFF & b);
	}
	private void pushShort(int i) {
		bytes[bdex++] = (byte) (i & 0xFF);
		bytes[bdex++] = (byte)((i >> 8) & 0xFF);
	}
	
	private void pushInt(long i) {
		bytes[bdex++] = (byte) (i & 0xFF);
		bytes[bdex++] = (byte)((i >>= 8) & 0xFF);
		bytes[bdex++] = (byte)((i >>= 8) & 0xFF);
		bytes[bdex++] = (byte)((i >>= 8) & 0xFF);
	}
	
	
	public void setChecking() {
		checking = true;
	}
	
	public int getNumFailed() {
		return numFailed;
	}
	
	
	private void pushOrTest(String a, String name) {
		if(checking) {
			if(a.equals(ll.remove())) {
				System.out.println("PASSED " + name);
			} else {
				System.out.println("FAILED " + name);
				numFailed++;
			}
		} else {
			ll.add(a);
		}
	}
	
	public XDPRequest getMsg() {
		int ndex = bdex;
		bdex = 0;
		pushShort(ndex); //Pktsize
		pushByte(11); //DeliveryFlag 11 indicates that it's an original message
		pushByte(ll.size()); //NumMsg
		pushInt(0); //SeqNum
		pushInt(getSeconds()); //sendTime
		pushInt(0); //sendtimeNs
		bdex = ndex;
		byte narr[] = new byte[bdex];
		for(int i = 0; i < bdex;i++)
			narr[i] = bytes[i];
		XDPRequest rq = new XDPRequest(narr,0);
		return rq;
	}
	
	/* THIS IS A METHOD THAT I'VE ADDED */
	public void executeOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long tradeId, int reason, long packetTimestamp) {
	/*	TSB s = new TSB();
		s.add(orderID);
		s.add(symbolIndex);
		s.add(time_ns);
		s.add(symbolSeqNumber);
		s.add(price);
		s.add(volume);
		s.add(isSell);
		s.add(tradeSession);
		s.add(packetTimestamp);
		pushOrTest(s.getStr(),"EXEC ORDER");
		if(!checking) {
			//Build the data
			pushShort(31);
			pushShort(100);
			pushInt(time_ns);
			pushInt(symbolIndex);
			pushInt(symbolSeqNumber);
			pushInt(orderID);
			pushInt(price);
			pushInt(volume);
			pushByte( isSell?'S':'B');
			pushByte(0); //OrderIDGTCIndicator -> ?????
			pushByte(tradeSession);
		}*/ 
		//TODO implement this 
	}
	
	public void addOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			int tradeSession, long packetTimestamp) throws SQLException {
		
		TSB s = new TSB();
		s.add(orderID);
		s.add(symbolIndex);
		s.add(time_ns);
		s.add(symbolSeqNumber);
		s.add(price);
		s.add(volume);
		s.add(isSell);
		s.add(tradeSession);
		s.add(packetTimestamp);
		if(!checking) {
			//Build the data
			pushShort(31);
			pushShort(100);
			pushInt(time_ns);
			pushInt(symbolIndex);
			pushInt(symbolSeqNumber);
			pushInt(orderID);
			pushInt(price);
			pushInt(volume);
			pushByte( isSell?'S':'B');
			pushByte(0); //OrderIDGTCIndicator -> ?????
			pushByte(tradeSession);
		}
		pushOrTest(s.getStr(),"ADD ORDER");
	}

	public void modifyOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, boolean isSell,
			long packetTimestamp) throws SQLException {
		TSB s = new TSB();
		s.add(orderID);
		s.add(symbolIndex);
		s.add(time_ns);
		s.add(symbolSeqNumber);
		s.add(price);
		s.add(volume);
		s.add(isSell);
		s.add(packetTimestamp);
		if(!checking) {
			//Build the data
			pushShort(31);
			pushShort(101);
			pushInt(time_ns);
			pushInt(symbolIndex);
			pushInt(symbolSeqNumber);
			pushInt(orderID);
			pushInt(price);
			pushInt(volume);
			pushByte( isSell?'S':'B');
			pushByte(0); //OrderIDGTCIndicator -> ?????
			pushByte(5); //Possible reason codes are 5 6 7
		}
		pushOrTest(s.getStr(),"MODIFY ORDER");
	}

	public void reduceOrderVolume(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long volumeReduction) throws SQLException {
		
	}

	public void deleteOrder(long orderID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long packetTimestamp) throws SQLException {
		TSB s = new TSB();
		s.add(orderID);
		s.add(symbolIndex);
		s.add(time_ns);
		s.add(symbolSeqNumber);

		s.add(packetTimestamp);
		if(!checking) {
			//Build the data
			pushShort(23);
			pushShort(102);
			pushInt(time_ns);
			pushInt(symbolIndex);
			pushInt(symbolSeqNumber);
			pushInt(orderID);
			pushByte('B'); //This could be either we just need a placeholder
			pushByte(0); //OrderIDGTCIndicator -> ?????
			pushByte(1); //Possible reason codes are 1 2 3
		}
		pushOrTest(s.getStr(),"DELETE ORDER");	
	}

	public void addTrade(long tradeID, long symbolIndex, long time_ns,
			long symbolSeqNumber, long price, long volume, long packetTimestamp)
			throws SQLException {
				
	}
			
	public void addStockSummary(long symbolIndex, long time_s, long time_ns,
			long highPrice, long lowPrice, long openPrice, long closePrice,
			long totalVolume) throws SQLException {
		
	}

	public void correctTrade(long originalTradeID, long tradeID,
			long symbolIndex, long time_s, long time_ns, long symbolSeqNumber,
			long price, long volume) throws SQLException {
		
	}

	public void addSourceTimeReference(long symbolIndex, long symbolSeqNumber,
			long referenceTime) throws SQLException {
		
	}

	public void addSymbolMappingEntry(long symbolIndex, String symbol,
			long priceScaleCode, long prevClosingPrice, long prevClosingVolume)
			throws SQLException {
		
	}

	public void cancelTrade(long tradeID, long symbolIndex, long time_s,
			long time_ns, long symbolSeqNumber) throws SQLException {
		
	}

	public void changeTradeSession(long symbolIndex, long time_s, long time_ns,
			long symbolSeqNumber, int tradingSession) throws SQLException {
		
	}

	public void commit() throws SQLException {
	
	}

	public void setConnection(Connection connection) {
		
	}

	@Override
	public ResultSet getTradesInTimeRangeForSymbol(long symbolID, long start,
			long end) throws SQLException {
		return null;
	}
	

}

public class XDPProcessingTest {

	static boolean runTests() {
		TestDatabase db = new TestDatabase();
		XDPProcessorUnit xdp = new XDPProcessorUnit(db);

		//TODO push some objects into the packet on which to test
		try {
			db.addOrder(0, 0, 0, 0, 0, 0, false, 0, db.getSeconds());
			db.addOrder(10, 0, 33, 0, 80, 0, false, 0, db.getSeconds());
			db.modifyOrder(5, 3, 43, 0, 10, 0, false, db.getSeconds());
			db.modifyOrder(5, 8, 2, 0, 660, 0, false, db.getSeconds());
			db.deleteOrder(522, 48, 2, 70, db.getSeconds());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	
		//Now test!
		db.setChecking();
		XDPRequest rq = db.getMsg();
		System.out.println("Size : " + rq.getPacketData().length);
		xdp.decode(rq);
		
		if(0 == db.getNumFailed()) {
			System.out.println("ALL TESTS PASSED");
			return true;
		} else {
			System.out.println(db.getNumFailed() + " Tests failed");
		}
		return false;
	}
	
	public static void main( String args[] ) {
		if( runTests() )
			System.out.println("All tests passed successfully");
		else
			System.out.println("Some or all tests failed");
	}
}
