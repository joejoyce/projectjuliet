package uk.ac.cam.cl.juliet.slave.queryprocessing;

import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.QueryResponse;

/**
 * Runs parts of queries for the server
 * 
 * @author Dylan McDermott
 * 
 */
public interface QueryProcessor {
	/**
	 * Runs part of a query
	 * 
	 * @param p
	 *            the query packet sent from the server
	 * @return the response of the query to send back to the server
	 */
	public QueryResponse runQuery(QueryPacket p);
}