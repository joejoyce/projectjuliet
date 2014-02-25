package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.IOException;

public interface XDPDataStream {
	public XDPRequest getPacket() throws IOException;
	
	/**
	 * Set the skip Boundary to alter the rate at which packets are made available
	 * @param pSkipBoundary   If there is a time difference of more than 
	 * 			skipBoundary seconds between two consecutive packet timestamps,
	 * 			then the time between them is skipped to get a throughput and to 
	 * 			skip large periods of inactivity.
	 */
	public void setSkipBoundary(float pSkipBoundary);
	
	/**
	 * Gets the skip Boundary.
	 * If there is a time difference of more than 
	 * 	skipBoundary seconds between two consecutive packet timestamps,
	 * 	then the time between them is skipped to get a throughput and to 
	 * 	skip large periods of inactivity.
	 * @return
	 * 	    the skip boundry
	 */
	public float getSkipBoundary();
}