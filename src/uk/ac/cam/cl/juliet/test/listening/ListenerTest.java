package uk.ac.cam.cl.juliet.test.listening;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.slave.listening.Listener;

public class ListenerTest {
	public static ConcurrentHashMap<Long, Boolean> processed = new ConcurrentHashMap<Long, Boolean>();
	public static ConcurrentHashMap<Long, Boolean> responded = new ConcurrentHashMap<Long, Boolean>();
	static MockXDPProcessor xdp;
	static MockDistributor distributor;
	static Listener listener = new Listener();
	public static Boolean serverStarted;

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out
					.println("Usage: ListenerTest <number of packets> <send delay in ms> <xdp processor delay in ms>");
			return;
		}

		try {
			final int num = Integer.parseInt(args[0]);
			final int delay = Integer.parseInt(args[1]);
			final int xdpDelay = Integer.parseInt(args[2]);
			distributor = new MockDistributor();
			xdp = new MockXDPProcessor(xdpDelay);

			Thread distributorThread = new Thread() {
				public void run() {
					try {
						distributor.acceptClient();
						System.out.println("Distributing " + num + " requests");
						long time = System.currentTimeMillis();
						for (int i = 0; i < num; i++) {
							XDPRequest r = new XDPRequest(null, 11, 0);
							r.setPacketId(i);
							distributor.write(r);
							if (delay > 0) {
								try {
									Thread.sleep(delay);
								} catch (InterruptedException e) {
								}
							}
						}
						System.out.println("Distributed all requests");
						while (responded.size() < num)
							;
						System.out.println("All requests responded to in "
								+ (System.currentTimeMillis() - time) + "ms");
						System.exit(0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			distributorThread.start();

			Thread listenerThread = new Thread() {
				public void run() {
					try {
						listener.listen("127.0.0.1", 5000, null, xdp, null);
					} catch (IOException | SQLException e) {
						e.printStackTrace();
					}
				}
			};
			listenerThread.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
