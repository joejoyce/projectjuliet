package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;

/**
 * @description WebServerListener
 * Waits for connections from the WebServer and passes
 * the socket over to the WebServerQueryHandler for processing.
 * 
 * @author Scott Williams
 */
public class WebServerListener implements Runnable {
	private ServerSocket querySocket;
	private Connection con;
	
	public WebServerListener(int querySocketPort, Connection con) throws IOException {
		this.querySocket = new ServerSocket(querySocketPort);
		this.con = con;
		Thread listener = new Thread(this);
		listener.start();
	}
	
	public void run() {		
		while(true) {
			try {
				Socket webserver = querySocket.accept();
				WebServerQueryHandler wqh = new WebServerQueryHandler(webserver, con);
				Thread t = new Thread(wqh);
				t.start();
			} catch (IOException e) {
				System.err.println("Error creating webserver socket");
				e.printStackTrace();
			}
		}
	}
}
