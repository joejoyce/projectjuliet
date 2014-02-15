package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.IOException;

public interface XDPDataStream {
	public XDPRequest getPacket() throws IOException;
}