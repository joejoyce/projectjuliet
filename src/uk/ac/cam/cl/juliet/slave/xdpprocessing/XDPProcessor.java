package uk.ac.cam.cl.juliet.slave.xdpprocessing;

import uk.ac.cam.cl.juliet.common.XDPRequest;

public interface XDPProcessor {
	/**
	 * 
	 * @param packet	The packet it should decode
	 * @return 	true if the packet was decoded successfully and its
	 * 			information has been written to the database,
	 * 			returns false otherwise
	 */
	public boolean decode (XDPRequest packet);
}