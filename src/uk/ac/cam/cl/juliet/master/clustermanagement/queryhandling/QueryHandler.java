package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import uk.ac.cam.cl.juliet.common.QueryPacket;
/**
 * @description QueryHandler Interface
 * A QueryHandler must enable Queries to run via the Distributor
 * 
 * @author Scott Williams
 */
public interface QueryHandler {
	// Accepts a QueryPacket to run
    public void runQuery(QueryPacket p, int id);
}
