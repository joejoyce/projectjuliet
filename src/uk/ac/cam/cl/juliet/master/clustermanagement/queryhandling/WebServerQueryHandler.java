package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
			BufferedReader din  = new BufferedReader(new InputStreamReader(new BufferedInputStream(server.getInputStream())));
			String query = din.readLine();
			System.out.println("Received query from server: " + query);			
			
			//TODO: Actually handle the query
			
			PrintWriter pw = new PrintWriter(server.getOutputStream(), true);
			pw.write("GabeN");
			pw.flush();
			System.out.println("Written GabeN");			
			pw.close();
			server.close();
		} catch (IOException e) {
			System.out.println("Error reading query from webserver");
			e.printStackTrace();
		}
    }
	
	public void runQuery(QueryPacket p, int id) {
    	
    }
}
