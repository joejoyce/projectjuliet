package uk.ac.cam.cl.juliet.slave.xdpprocessing;

import java.util.LinkedList;
import java.util.Queue;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Message;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.Packets.Packet;

public class XDPProcessorUnit implements XDPProcessor {

	@Override
	public boolean decode(XDPRequest packet) {
		boolean result = true;
		
		Packet currentPacket = new Packet(packet.getPacketData());
		
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

	private boolean decodeSymbolMappingMessage(Message pMessage) {
		
		//TODO
		return false;
	}

}
