package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.sql.SQLException;

import uk.ac.cam.cl.juliet.slave.distribution.DatabaseConnectionUnit;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessorUnit;

/**
 * Has the main method for the slaves. Starts the listener.
 * @author Dylan McDermott
 *
 */
public class Client {
    public static void main(String[] args) {
    	Listener listener = new Listener();
    	try {
    		listener.listen(args[0], 500, new XDPProcessorUnit(new DatabaseConnectionUnit()), null); // TODO: replace null with query processor
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(1);
		} catch (SQLException e) {
			System.err.println("A database error occurred.");
			e.printStackTrace();
			System.exit(1);
		}
    }
}
