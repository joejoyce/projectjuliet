package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPPacket;
import java.io.IOException;

public interface XDPDataStream {
	public XDPPacket getPacket() throws IOException;
	// Won't be used with the static sample dataset
	public XDPPacket requestPacket(long packetID);
}