package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnection;
import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnectionUnit;
import uk.ac.cam.cl.juliet.slave.queryprocessing.QueryProcessorUnit;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;

/**
 * Has the main method for the slaves. Starts the listener.
 * 
 * @author Dylan McDermott
 * 
 */
public class Client {
	public static void main(String[] args) {
		Debug.registerOutputLocation(System.out);
		Debug.setPriority(Debug.ERROR);
		
		Thread t = new Thread () {
			public void run() {
				Scanner s = new Scanner(System.in);
				String input = "";
				while (input != "quit") {
					input = s.nextLine();
					if (Debug.parseDebugArgs(input)) {
						continue;
					}
				}
				s.close();
				System.exit(0);
			}
		};	
		t.setDaemon(true);
		t.start();
		
		int sleepDelay = 2000;
		for(int i = 0; i < 5; i++) {
			try {
				Debug.println(Debug.INFO,"Spawning new listener and everything!!!!!");
				Listener listener = new Listener();
				Connection c = DriverManager.getConnection("jdbc:mysql://" + args[0] + ":3306/juliet?rewriteBatchedStatements=true&useServerPrepStmts=false", "root", "rootword");
				DatabaseConnection db = new DatabaseConnectionUnit(c);
				listener.listen(args[0], 5000, db, new XDPProcessorUnit(db), new QueryProcessorUnit(db));
				sleepDelay = 2000;
			} catch (IOException e) {
				System.err.println("An error occurred communicating with the server.");
				e.printStackTrace();
				sleepDelay *= 2;
			} catch (SQLException e) {
				System.err.println("A database error occurred.");
				e.printStackTrace();
				sleepDelay *= 2;
			}
			try {
				Thread.sleep(sleepDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
