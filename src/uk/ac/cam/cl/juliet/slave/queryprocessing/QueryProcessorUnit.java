package uk.ac.cam.cl.juliet.slave.queryprocessing;

import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;

/**
 * Handles all query packets sent from the master to the slave.
 * 
 * @author Dylan McDermott
 *
 */
public class QueryProcessorUnit implements QueryProcessor {
	
	private DatabaseConnection connection;
	
	public QueryProcessorUnit(DatabaseConnection connection) {
		this.connection = connection;
	}
	
	@Override
	public QueryPacket runQuery(QueryPacket p) {
		// TODO Auto-generated method stub
		return null;
	}
    
}
