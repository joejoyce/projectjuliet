package uk.ac.cam.cl.juliet.slave.xdpprocessing;

import uk.ac.cam.cl.juliet.common.XDPRequest;
/**
 * This is the interface for the part that will decode the XDP packets on the 
 * Raspberry Pis.
 * @author Lucas Sonnabend
 *
 */
public interface XDPProcessor {
	/**
	 * Decodes the data that is within the XDPRequest packet into messages
	 * and writes the result into the database.
	 * 
	 * @param packet	The packet it should decode
	 * @return 	true if the packet was decoded successfully and its
	 * 			information has been written to the database,
	 * 			returns false otherwise
	 */
	public boolean decode (XDPRequest packet);
}
