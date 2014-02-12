package uk.ac.cam.cl.juliet.test.listening;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.XDPResponse;

public class MockDistributor {
	ObjectOutputStream output;
	ObjectInputStream input;
	ServerSocket server;

	public MockDistributor() throws IOException {
		server = new ServerSocket(5000);
	}

	public void write(Container c) throws IOException {
		this.output.writeObject(c);
	}

	private void receiveContainer() {
		try {
			XDPResponse r = (XDPResponse) input.readObject();
			if (!ListenerTest.processed.containsKey(r.getPacketId()))
				System.out
						.println("Response "
								+ r.getPacketId()
								+ " received before the XDP processor processed the packet.");
			ListenerTest.responded.put(r.getPacketId(), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void acceptClient() throws IOException {
		Socket client = server.accept();

		output = new ObjectOutputStream(client.getOutputStream());
		input = new ObjectInputStream(client.getInputStream());
		output.writeObject(new ConfigurationPacket());
		output.flush();

		Thread receiveThread = new Thread() {
			public void run() {
				while (true)
					receiveContainer();
			}
		};
		receiveThread.start();
	}
}
