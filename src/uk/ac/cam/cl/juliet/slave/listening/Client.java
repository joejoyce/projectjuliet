package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Has the main method for the slaves. Starts the listener.
 * 
 * @author Dylan McDermott
 * 
 */
public class Client {
	public static void main(String[] args) {
		Listener listener = new Listener();
		try {
			listener.listen(args[0], 5000);
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(0);
		} catch (SQLException e) {
			System.err.println("A database error occurred.");
			e.printStackTrace();
			System.exit(0);
		}
	}
}
