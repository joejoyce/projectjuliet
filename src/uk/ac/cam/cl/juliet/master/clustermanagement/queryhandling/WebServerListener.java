package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import uk.ac.cam.cl.juliet.common.Debug;

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
		this.querySocket = new ServerSocket(querySocketPort, 10, InetAddress.getByName("127.0.0.1"));
		this.con = con;
		Thread listener = new Thread(this);
		listener.start();
	}
	
	public void run() {		
		while(true) {
			try {
				Debug.println("Waiting for connection");
				Socket webserver = querySocket.accept();
				Debug.println("Accepted new connection");
				WebServerQueryHandler wqh = new WebServerQueryHandler(webserver, con);
				Thread t = new Thread(wqh);
				t.start();
			} catch (IOException e) {
				System.err.println("Error creating webserver socket");
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/juliet", "root", "rootword");
		new WebServerListener(1337, con);	
	}
}
