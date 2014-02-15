package uk.ac.cam.cl.juliet.test.listening;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;

public class MockXDPProcessor implements XDPProcessor {

	private int delay;

	public MockXDPProcessor(int delay) {
		this.delay = delay;
	}

	@Override
	public boolean decode(XDPRequest packet) {
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
		}
		if (ListenerTest.processed.containsKey(packet.getPacketId())
				|| ListenerTest.processed.containsKey(packet.getPacketId()))
			System.out.println("Packet " + packet.getPacketId()
					+ " processed more than once");
		ListenerTest.processed.put(packet.getPacketId(), true);
		return true;
	}

}
