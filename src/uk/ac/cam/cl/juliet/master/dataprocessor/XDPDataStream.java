package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPRequest;

import java.io.IOException;
import java.util.Map;

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
	 * 	    the skip boundary
	 */
	public float getSkipBoundary();
	
	/**
	 * This method shall only be called by the #ShutdownSettingsSaver thread.
	 * It returns a map of settings, containing the current position of the file
	 * pointer.
	 * Furthermore it closes the inputStreams from the files and sets a flag so
	 * that any further getPacket requests will return null.
	 * @return
	 */
	public Map<String, String> endAndGetSettings();
}