package uk.ac.cam.cl.juliet.slave.xdpprocessing;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Message;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Packet;

public class XDPProcessorUnit implements XDPProcessor {

	@Override
	public boolean decode(XDPRequest packet) {
		boolean result = true;
		
		Packet currentPacket = new Packet(packet.getPacketData());
		// Check whether the packet is a new one (deliveryFlag 11)
		// or a retransmission/error 
		// TODO: should be pushed into the data processor for performance reasons??
		if(currentPacket.getDeliveryFlag() != 11) {	
			return true;
		}
		
		Message m = currentPacket.getNextMessage();
		
		while(m != null) {
			switch( m.getMessageType()) {
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
			case 222:
				result &= TradeCancelOrBustMessage(m);
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
	
	private boolean StockSummaryMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean TradeCancelOrBustMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeOrderBookExecutionMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeOrderBookDeleteOrderMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeOrderBookModifyOrderMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeOrderBookAddOrderMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeTradeSessionChangeMessage(Message m) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeSymbolMappingMessage(Message m) {
		
		int symbolIndex = (int) m.readLong(4);
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
		
		//TODO write to the database
		
		return true;
	}

}
