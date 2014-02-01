package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import uk.ac.cam.cl.juliet.common.QueryPacket;
/**
 * @description WebServerQueryHandler
 * Extracts a query from a WebServer connection socket and 
 * decides what to do. This may involve sending the QueryPacket
 * off to the Distributor. 
 * 
 * @author Scott Williams
 */
public class WebServerQueryHandler implements QueryHandler, Runnable {
    private Socket server;
	
	public WebServerQueryHandler(Socket server) {
		this.server = server;
    }
	
	public void run() {
		try {
			BufferedInputStream in = new BufferedInputStream(server.getInputStream());
			BufferedReader din  = new BufferedReader(new InputStreamReader(in));
			String query = din.readLine();
			System.out.println("Received query: " + query);
			//TODO decide how to handle the query...
		} catch (IOException e) {
			System.out.println("Error reading query from webserver");
			e.printStackTrace();
		}
    }
	
	public void runQuery(QueryPacket p, int id) {
    	
    }
}
