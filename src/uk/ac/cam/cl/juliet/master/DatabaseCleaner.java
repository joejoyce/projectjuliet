package uk.ac.cam.cl.juliet.master;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A class which periodically runs a method to remove data which is no longer
 * needed from the database.
 * 
 * @author Dylan McDermott
 * 
 */
public class DatabaseCleaner implements Runnable {
	private Connection connection;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> future;

	/**
	 * Schedules the run method to run periodically.
	 * 
	 * @param con
	 *            A connection to the database.
	 */
	public DatabaseCleaner(Connection con) {
		this.connection = con;
		this.executor = Executors.newScheduledThreadPool(1);
		this.future = executor.scheduleWithFixedDelay(this, 0, 10,
				TimeUnit.SECONDS);
	}

	/**
	 * Removes data which is no longer needed once.
	 */
	public void run() {
		try {
			PreparedStatement statement = connection
					.prepareStatement("LOCK TABLES order_book WRITE");
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			PreparedStatement s = connection
					.prepareStatement("DELETE FROM order_book WHERE added = 1 AND deleted = 1");
			s.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			PreparedStatement statement = connection
					.prepareStatement("LOCK TABLES trade WRITE");
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			PreparedStatement s = connection
					.prepareStatement("DELETE FROM trade WHERE added = 1 AND deleted = 1");
			s.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			PreparedStatement statement = connection
					.prepareStatement("UNLOCK TABLES");
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops the run method from being executed.
	 */
	public void stop() {
		future.cancel(false);
	}
}
