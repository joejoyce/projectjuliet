package uk.ac.cam.cl.juliet.slave.xdpprocessing;

import java.sql.SQLException;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Message;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Packet;

public class XDPProcessorUnit implements XDPProcessor {
	
	private DatabaseConnection mDB;
	
	public XDPProcessorUnit(DatabaseConnection pDBConnection) {
		mDB = pDBConnection;
	}

	@Override
	public boolean decode(XDPRequest packet) {
		boolean result = true;
		
		Packet currentPacket = new Packet(packet.getPacketData());
		Message m = currentPacket.getNextMessage();
		
		while(m != null) {
			switch( m.getMessageType()) {
			case 2:
				result &= decodeSourceTimeReferenceMessage(m);
				break;
			case 3:
				result &= decodeSymbolMappingMessage(m);
				break;
			case 33:
				result &= decodeTradeSessionChangeMessage(m);
				break;
			case 100:
				result &= decodeOrderBookAddOrderMessage(m);
				break;
			case 101:
				result &= decodeOrderBookModifyOrderMessage(m);
				break;
			case 102:
				result &= decodeOrderBookDeleteOrderMessage(m);
				break;
			case 103:
				result &= decodeOrderBookExecutionMessage(m);
				break;
			case 221:
				result &= decodeTradeCancelOrBustMessage(m);
			case 222:
				result &= decodeTradeCorrectionMessage(m);
				break;
			case 223:
				result &= StockSummaryMessage(m);
				break;
			default:
				// An unsupported message has been seen,
				// therefore, do nothing
			}
			m = currentPacket.getNextMessage();
		}

		return result;
	}
	
	private boolean decodeSourceTimeReferenceMessage(Message m) {
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		long timeReference = m.readLong(4);
		try {
			mDB.addSourceTimeReference(symbolIndex, symbolSequenceNumber, timeReference);
		} catch (SQLException e) {
			// TODO write to log?
			return false;
		}
		return true;
	}

	private boolean decodeTradeCorrectionMessage(Message m) {
		
		long sourceTime_s = m.readLong(4);
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		long originalTradeID = m.readLong(4); 	//this is the name from the spec. A 
												//better name would be orignialSequenceNumber 
		long tradeID = m.readLong(4);
		long price = m.readLong(4);
		long volume = m.readLong(4);
		// there are 5 more bytes in the message containing trade conditions
		// but we are not interested in those
		
		try {
			mDB.correctTrade(originalTradeID, tradeID, symbolIndex,
					sourceTime_s, sourceTime_ns, symbolSequenceNumber,
					price, volume);
		} catch (SQLException e) {
			// TODO write to log?
			return false;
		}
		return true;
	}

	private boolean StockSummaryMessage(Message m) {
		long sourceTime_s = m.readLong(4);
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long highPrice = m.readLong(4);
		long lowPrice = m.readLong(4);
		long openPrice = m.readLong(4);
		long closePrice = m.readLong(4);
		long totalVolume = m.readLong(4);
		
		try {
			mDB.addStockSummary(symbolIndex, sourceTime_s, sourceTime_ns,
					highPrice, lowPrice, openPrice, closePrice, totalVolume);
		} catch (SQLException e) {
			// TODO write to error log
			return false;
		}
		
		//write to database
		return true;
	}

	private boolean decodeTradeCancelOrBustMessage(Message m) {
		long sourceTime_s = m.readLong(4);
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long SymbolSequenceNumber = m.readLong(4);
		long originalTradeID = m.readLong(4);
		
		try {
			mDB.cancelTrade(originalTradeID, symbolIndex, sourceTime_s, sourceTime_ns, SymbolSequenceNumber);
		} catch (SQLException e) {
			// TODO write to error log or something similar?
			return false;
		}
		return true;
	}

	private boolean decodeOrderBookExecutionMessage(Message m) {
		long sourcetime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		long orderID = m.readLong(4);
		long price = m.readLong(4);
		long volume = m.readLong(4);
		boolean isGTC = (m.readUnsignedByte()== 1);
		int reasonCode = m.readUnsignedByte();
		long tradeID = m.readLong(4);
		
		try {
			mDB.addTrade(tradeID, symbolIndex, sourcetime_ns, 
					symbolSequenceNumber, price, volume);
			// if the reasonCode is not zero, no extra modify or delete message 
			//will be sent for that order:
			if (reasonCode == 7) {
				//an order is partially filled
				mDB.reduceOrderVolume(orderID, symbolIndex, sourcetime_ns,
						symbolSequenceNumber, volume);
			} else if (reasonCode == 3) {
				//an order is fully executed
				mDB.deleteOrder(orderID, symbolIndex, sourcetime_ns,
						symbolSequenceNumber);
			} else {
				//the reasonCode is invalid
				//TODO throw an exception or have an error log!
			}
			
		} catch (SQLException e) {
			// TODO write to error log?
			return false;
		}
		return true;
	}

	private boolean decodeOrderBookDeleteOrderMessage(Message m) {
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		long orderID = m.readLong(4);
		boolean isSell = m.readChar()== 'B';
		// there are two more bytes containing OrderIDGTCIndicator and a ReasonCode
		// but we don't need them now

		try {
			mDB.deleteOrder(orderID, symbolIndex, sourceTime_ns, symbolSequenceNumber);
		} catch (SQLException sqle) {
			//TODO print to an error log?
			return false;
		}
		return true;
	}

	private boolean decodeOrderBookModifyOrderMessage(Message m) {
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long SymbolSequenceNumber = m.readLong(4);
		long orderID = m.readLong(4);
		long price = m.readLong(4);
		long volume = m.readLong(4);
		boolean isSell = m.readChar()== 'B';
		// there are two more bytes containing OrderIDGTCIndicator and a ReasonCode
		
		try {
			mDB.modifyOrder(orderID, symbolIndex, sourceTime_ns,
					SymbolSequenceNumber, price, volume, isSell);
		} catch (SQLException e) {
			// TODO write to an error log?
			return false;
		}
		return true;
	}

	private boolean decodeOrderBookAddOrderMessage(Message m) {
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		long orderID = m.readLong(4);
		long price = m.readLong(4);
		long volume = m.readLong(4);
		boolean isSell = m.readChar()== 'S';
		boolean isGTC = (m.readUnsignedByte()== 1);
		int tradeSession = m.readUnsignedByte();
		
		try {
			mDB.addOrder(orderID, symbolIndex, sourceTime_ns, 
					symbolSequenceNumber, price, volume, isSell, tradeSession);
		} catch (SQLException e) {
			// TODO print to error log?
			return false;
		}
		return true;
	}

	private boolean decodeTradeSessionChangeMessage(Message m) {
		long sourceTime_s = m.readLong(4);
		long sourceTime_ns = m.readLong(4);
		long symbolIndex = m.readLong(4);
		long symbolSequenceNumber = m.readLong(4);
		int tradingSession = m.readUnsignedByte();
		// TODO update database
		try {
			mDB.changeTradeSession(symbolIndex, sourceTime_s, sourceTime_ns,
					symbolSequenceNumber, tradingSession);
		} catch (SQLException e) {
			// TODO write to error-log or something?
						return false;
		}
		return false;
	}

	private boolean decodeSymbolMappingMessage(Message m) {
		
		long symbolIndex = m.readLong(4);
		String symbol = m.readString(11); 
		// Jump the filler
		m.readChar();
		long marketID = m.readLong(2);
		long systemID = m.readLong(1);
		
		long exchangeCode = m.readChar();
		long priceScaleCode = m.readLong(1);
		long securityType = m.readChar();
		long lotSize = m.readLong(2);

		long prevClosePrice = m.readLong(4);
		long prevCloseVolume = m.readLong(4);
		long priceResolution = m.readLong(1);
		long roundLot = m.readChar();
		
		try {
			mDB.addSymbolMappingEntry(symbolIndex, symbol, priceScaleCode, 
					prevClosePrice, prevCloseVolume);
		} catch (SQLException e) {
			// TODO write to error-log or something?
			return false;
		}
		return true;
	}

}
