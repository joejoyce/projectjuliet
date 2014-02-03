package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;

/**
 * Has the main method for the slaves. Starts the listener.
 * @author Dylan McDermott
 *
 */
public class Client {
    public static void main(String[] args) {
    	Listener listener = new Listener();
    	try {
    		listener.listen(args[0], 500, null, null); // TODO: replace nulls with XDP processor and query processor
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(1);
		}
    }
}
