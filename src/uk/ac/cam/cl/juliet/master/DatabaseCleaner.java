package uk.ac.cam.cl.juliet.master;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DatabaseCleaner implements Runnable {
	private Connection connection;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;

	public DatabaseCleaner(Connection con) {
		this.connection = con;
		this.executor = Executors.newScheduledThreadPool(1);
		this.future = executor.scheduleWithFixedDelay(this, 0, 10,
				TimeUnit.SECONDS);
	}

	public void run() {
		try {
			PreparedStatement s = connection
					.prepareStatement("DELETE FROM order_book WHERE added = 1 AND deleted = 1");
			s.execute();
		} catch (SQLException e) {
		}
		
		try {
			PreparedStatement s = connection
					.prepareStatement("DELETE FROM trade WHERE added = 1 AND deleted = 1");
			s.execute();
		} catch (SQLException e) {
		}
	}
	
	public void stop() {
		future.cancel(false);
	}
}
