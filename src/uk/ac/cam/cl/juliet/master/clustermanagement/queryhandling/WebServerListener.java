package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @description WebServerListener
 * Waits for connections from the WebServer and passes
 * the socket over to the WebServerQueryHandler for processing.
 * 
 * @author Scott Williams
 */
public class WebServerListener implements Runnable {
	private ServerSocket querySocket;
	
	public WebServerListener(int querySocketPort) throws IOException {
		this.querySocket = new ServerSocket(querySocketPort);
		Thread listener = new Thread(this);
		listener.start();
	}
	
	public void run() {		
		while(true) {
			try {
				System.out.println("Waiting for connection");
				Socket webserver = querySocket.accept();
				System.out.println("Accepted new connection");
				WebServerQueryHandler wqh = new WebServerQueryHandler(webserver);
				Thread t = new Thread(wqh);
				t.start();
			} catch (IOException e) {
				System.err.println("Error creating webserver socket");
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		new WebServerListener(1337);		
	}
}
