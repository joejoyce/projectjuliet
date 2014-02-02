package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.cam.cl.juliet.common.Container;

/**
 * The listener reads packets send from the master PC and passes them on to
 * either the XDP processor or query handler.
 * 
 * @author Dylan McDermott
 * 
 */
public class Listener {
	private Socket socket;
	private Thread receiveThread;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private LinkedBlockingQueue<Container> responseQueue;

	/**
	 * Connects to the server and begins to read and process packets.
	 * 
	 * @param server
	 *            The server to connect to.
	 * @param port
	 *            The port which packets are being sent from.
	 * @throws IOException
	 */
	public void listen(String server, int port) throws IOException {
		this.socket = new Socket(server, port);
		this.input = new ObjectInputStream(this.socket.getInputStream());
		this.output = new ObjectOutputStream(this.socket.getOutputStream());

		this.receiveThread = new Thread() {
			@Override
			public void run() {
				while (true)
					processPacket();
			}
		};
		this.receiveThread.start();

		// Sends any waiting responses back to the server.
		while (true) {
			try {
				Container response = responseQueue.take();
				output.writeObject(response);
				output.flush();
			} catch (InterruptedException e) {
			}
		}
	}

	private void processPacket() {
    	try {
			Container container = (Container)this.input.readObject();
			
			
		} catch (ClassNotFoundException | ClassCastException e) {
			System.err.println("An unexpected object was recieved from the server.");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(1);
		}
    	
    }
}
