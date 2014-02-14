package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

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
		Debug.setPriority(-50); // Default priority is 5

		Listener listener = new Listener();
		try {
			DatabaseConnection db = new DatabaseConnectionUnit(
					DriverManager.getConnection("jdbc:mysql://" + args[0]
							+ ":3306/juliet", "root", "rootword"));
			listener.listen(args[0], 5000, db, new XDPProcessorUnit(db),
					new QueryProcessorUnit(db));
		} catch (IOException e) {
			System.err
					.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(0);
		} catch (SQLException e) {
			System.err.println("A database error occurred.");
			e.printStackTrace();
			System.exit(0);
		}
	}
}
