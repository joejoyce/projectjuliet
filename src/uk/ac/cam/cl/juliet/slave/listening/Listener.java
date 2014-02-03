package uk.ac.cam.cl.juliet.slave.listening;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.common.StringTestPacket;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.common.XDPResponse;
import uk.ac.cam.cl.juliet.slave.xdpprocessing.XDPProcessor;

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
	private XDPProcessor xdp;

	/**
	 * Connects to the server and begins to read and process packets.
	 * 
	 * @param server
	 *            The server to connect to.
	 * @param port
	 *            The port which packets are being sent from.
	 * @param xdp
	 *            An XDP processor to send any XDP packets to.
	 * @throws IOException
	 */
	public void listen(String server, int port, XDPProcessor xdp)
			throws IOException {
		this.socket = new Socket(server, port);
		this.input = new ObjectInputStream(this.socket.getInputStream());
		this.output = new ObjectOutputStream(this.socket.getOutputStream());
		this.xdp = xdp;

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
				synchronized (output) {
					output.writeObject(response);
					output.flush();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	private void processPacket() {
		try {
			Container container = (Container) this.input.readObject();

			if (container instanceof XDPRequest) {
				processXDPRequest((XDPRequest) container);
			} else if (container instanceof QueryPacket) {
				// TODO
			} else if (container instanceof StringTestPacket) {
				System.out.println(container);
			} else {
				// TODO: unknown packet - throw exception?
			}
		} catch (ClassNotFoundException | ClassCastException e) {
			System.err
					.println("An unexpected object was recieved from the server.");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err
					.println("An error occurred communicating with the server.");
			e.printStackTrace();
			System.exit(1);
		}

		// If the response queue is getting too big, send all responses
		if (this.responseQueue.size() > 10) { 
			synchronized (output) {
				try {
					Container response;
					while ((response = this.responseQueue.poll()) != null)
						output.writeObject(response);
					output.flush();
				} catch (IOException e) {
					System.err
							.println("An error occurred sending a response to the server");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	private void processXDPRequest(XDPRequest container) {
		boolean result = this.xdp.decode(container);
		XDPResponse response = new XDPResponse(container.getPacketId(), result);
		responseQueue.add(response);
	}
}
