package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.cl.juliet.common.CandlestickRequest;
import uk.ac.cam.cl.juliet.common.CandlestickResponse;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.LatencyMonitor;
import uk.ac.cam.cl.juliet.common.MovingAverageRequest;
import uk.ac.cam.cl.juliet.common.MovingAverageResponse;
import uk.ac.cam.cl.juliet.common.PriceToClearQuery;
import uk.ac.cam.cl.juliet.common.PriceToClearResponse;
import uk.ac.cam.cl.juliet.common.StockStatisticsRequest;
import uk.ac.cam.cl.juliet.common.StockStatisticsResponse;
import uk.ac.cam.cl.juliet.master.ClusterServer;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;
import uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling.SpikeDetectionRunnable.Spike;

/**
 * @description WebServerQueryHandler Extracts a query from a WebServer
 *              connection socket and decides what to do. This may involve
 *              sending the QueryPacket off to the Distributor.
 * 
 * @author Scott Williams
 */
public class WebServerQueryHandler implements QueryHandler, Runnable {
	private Socket server;
	private Connection con;

	private NotificationsList notifications = new NotificationsList();
	private OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

	/**
	 * Creates a new query handler.
	 * 
	 * @param server
	 *            The web server which requests are received from and responses
	 *            sent to.
	 * @param con
	 *            A connection to the database.
	 */
	public WebServerQueryHandler(Socket server, Connection con) {
		this.server = server;
		this.con = con;
	}

	/**
	 * Receives a query from the web server, processes it, and then sends back a
	 * response.
	 */
	public void run() {
		try {
			BufferedReader din = new BufferedReader(new InputStreamReader(new BufferedInputStream(server.getInputStream())));
			PrintWriter pw = new PrintWriter(server.getOutputStream(), true);

			String query = din.readLine();
			String[] splitQuery = query.split("\\|");

			switch (splitQuery[0]) {
			case "basic":
				runBasicQuery(splitQuery[1], pw);
				break;
			case "status":
				runStatusQuery(splitQuery[1], pw);
				break;
			case "config":
				runConfigQuery(splitQuery[1], pw);
				break;
			case "cluster":
				runClusterQuery(splitQuery[1], pw);
				break;
			case "statistics":
				runStatisticsQuery(splitQuery[1], pw);
				break;
			case "spikes":
				runSpikeDetectionQuery(splitQuery[1], pw);
				break;
			case "notifications":
				runNotificationsQuery(splitQuery[1], pw);
				break;
			default:
				Debug.println(Debug.ERROR, "Unsupported query type");
			}

			pw.flush();
			pw.close();
			server.close();
		} catch (IOException e) {
			Debug.println(Debug.ERROR, "Error reading query from webserver");
			e.printStackTrace();
		}
	}

	/**
	 * Gets all newly detected Spikes from the spike detector, converts them
	 * into json objects and outputs them into the PrintWriter pw
	 * 
	 * @param string
	 * @param pw
	 *            output for the json objects
	 */
	private void runSpikeDetectionQuery(String string, PrintWriter pw) {
		Debug.println(Debug.INFO, "Running a spike detection query");
		SpikeDetectionRunnable spikeDetector = ClusterServer.spikeDetector;
		ConcurrentLinkedQueue<Spike> listOfSpikes = spikeDetector.getSpikeBuffer();

		JsonBuilder jb = new JsonBuilder();
		jb.stArr();
		while (!listOfSpikes.isEmpty()) {
			Spike s = listOfSpikes.poll();
			jb.stOb();
			jb.pushPair("symbol", s.getSymbol());
			jb.pushPair("timeOfSpike", s.getTime());
			jb.finOb();
		}
		jb.finArr();
		String jsonResponse = jb.toString();
		Debug.println(Debug.DEBUG, "spike query result" + jsonResponse);
		pw.print(jsonResponse);

	}

	private void runStatisticsQuery(String symbolID, final PrintWriter pw) {
		Debug.println(Debug.INFO, "Running a stock statistics query");

		Callback statisticsCallback = new Callback() {
			@Override
			public void callback(Container data) {
				StockStatisticsResponse response = (StockStatisticsResponse) data;
				JsonBuilder jb = new JsonBuilder();
				jb.stArr();

				jb.stOb();
				jb.pushPair("lastTrade", response.lastTradePrice);
				jb.pushPair("totalVolume", response.totalTradeVolume);
				jb.pushPair("highestTrade", response.highestTradePrice);
				jb.pushPair("lowestTrade", response.lowestTradePrice);
				jb.pushPair("spread", response.spread);
				jb.pushPair("change", response.change);
				jb.finOb();

				jb.finArr();
				String jsonResponse = jb.toString();
				Debug.println(Debug.DEBUG, "Statistics query result" + jsonResponse);
				pw.print(jsonResponse);
				this.finished = true;
			}
		};
		long symbol_id = Long.parseLong(symbolID);
		ClusterMaster cm = ClusterServer.cm;
		try {
			cm.sendPacket(new StockStatisticsRequest(symbol_id), statisticsCallback);
		} catch (NoClusterException e) {
			Debug.println(Debug.ERROR, "Cluster query exception while trying to execute" + "a stock statistics query.");
		}
		statisticsCallback.waitUntilDone();
	}

	/**
	 * Runs a query which returns some information about the status of the
	 * system.
	 * 
	 * @param query
	 *            A string containing the query to run.
	 * @param pw
	 *            The writer to write the response to.
	 */
	public void runStatusQuery(String query, PrintWriter pw) {
		Debug.println("Running a status query");
		ClusterMaster cm = ClusterServer.cm;

		if (query.equals("time")) {
			long t = cm.getTime();
			pw.write(t + "");
			return;
		}

		if (query.equals("latency")) {
			LatencyMonitor lm = new LatencyMonitor();
			LatencyMonitorCallback cb = new LatencyMonitorCallback(10); 
																		
			cb.numSentTo = cm.broadcast(lm, cb);
			cb.waitUntilDone();
			pw.write(cb.generateJson());
			return;
		}

		if (query.equals("throughput")) {
			int total = cm.getPacketThroughput();
			pw.write("{\"data\": \" " + total + " \"}");
			return;
		}

		JsonBuilder j = new JsonBuilder();

		j.stOb();
		Client carr[] = cm.listClients();
		JsonBuilder jb = new JsonBuilder();
		jb.stArr();
		for (int i = 0; i < carr.length; i++) {
			jb.stOb();
			jb.pushPair("name", carr[i].getClientIP().toString().substring(1));
			jb.pushPair("totalPackets", carr[i].getTotalWork());
			jb.pushPair("currentPackets", carr[i].getCurrentWork());
			jb.finOb();
		}
		jb.finArr();

		if (query.equals("clients")) {
			pw.write(jb.toString());
			return;
		}

		j.pushPairNoQuotes("clients", jb.toString());

		long t = cm.getTime();
		j.pushPair("time", t);

		int total = cm.getPacketThroughput();
		j.pushPair("throughput", total);

		// I think this might only work on linux?
		double la = os.getSystemLoadAverage();
		if (la >= 0)
			j.pushPair("loadAv", la);
		else
			j.pushPair("loadAv", "Not supported");

		try {
			Statement s = con.createStatement();
			ResultSet resOrders = s.executeQuery("SELECT count(*) from order_book");
			resOrders.next();
			String orders = resOrders.getString(1);
			j.pushPair("total_orders", orders);

			s = con.createStatement();
			ResultSet resTrades = s.executeQuery("SELECT count(*) from trade");
			resTrades.next();
			String trades = resTrades.getString(1);
			j.pushPair("total_trades", trades);

			s = con.createStatement();
			ResultSet resSymbols = s.executeQuery("SELECT count(*) from symbol");
			resSymbols.next();
			String symbols = resSymbols.getString(1);
			j.pushPair("total_symbols", symbols);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		j.finOb();

		String ret = j.toString();
		pw.write(ret);
	}

	/**
	 * Runs a query which changes the configuration of the system.
	 * 
	 * @param query
	 *            A string containing the query to run.
	 * @param pw
	 *            The writer to write the response to.
	 */
	public void runConfigQuery(String query, PrintWriter pw) {
		if (query.equals("pause")) {
			ClusterServer.dp.pause = !ClusterServer.dp.pause;
			System.out.println("paused");
			pw.write("");
			return;
		}

		if (query.equals("restart")) {
			System.out.println("Got query");
			ClusterServer.dp.restart();
			System.out.println("restasted");
			try {
				Statement s = con.createStatement();
				s.executeUpdate("delete from order_book");
				s = con.createStatement();
				s.executeUpdate("delete from trade");
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return;
		}

		// If it depends on rate needs to map to the distributor
		if (query.matches("\\s*set\\s+\\S+\\s*=\\s*\\S+\\s*")) {
			Pattern p = Pattern.compile("\\s*set\\s+(\\S+)\\s*=\\s*(\\S+)\\s*");
			Matcher m = p.matcher(query);
			if (m.find()) {
				String key = m.group(1);
				String value = m.group(2);
				if (key.equals("data.rate")) {
					// Need to pass it onto the data handler
					Debug.println(Debug.INFO, "Adjusting the data rate");
					// TODO make it change the data rate
					// if you mean changing the skip boundary to make the
					// XDPDataStream
					// run faster: -lucas
					ClusterServer.dp.getDataStream().setSkipBoundary(Float.parseFloat(value));
				} else {
					ClusterServer.cm.setSetting(key, value);
				}// Set a value
			} else {
				Debug.println(Debug.ERROR, "Invalid form for settings set string");
				pw.write("{}");
			}

		} else if (query.matches("\\s*get\\s+(\\S+)\\s*")) {
			Pattern p = Pattern.compile("\\s*get\\s+(\\S+)\\s*"); // TODO is
																	// this
																	// supposed
																	// to be
																	// s*get ??
			Matcher m = p.matcher(query);
			if (m.find()) {
				String key = m.group(1);
				JsonBuilder jb = new JsonBuilder();
				if (key.equals("data.rate")) {
					Debug.println(Debug.INFO, "Getting the data rate");
					// TODO make it retrieve the data rate
					// if you mean the skip boundary that indicates whether the
					// XDPDataStream
					// runs faster than real time: here it is:
					float skip = ClusterServer.dp.getDataStream().getSkipBoundary();
					pw.write("{\"skip\": \"" + skip + "\"}");
				} else {
					String vl = ClusterServer.cm.getSetting(key);
					vl = (null == vl) ? "" : vl;
					jb.mkPair(key, vl);
					pw.write(jb.toString());
				}
			} else {
				Debug.println(Debug.ERROR, "Invalid form for settings get string");
				pw.write("{}");
			}

		} else if (query.trim().equals("list")) {
			// List settings
			// TODO get current rate
			JsonBuilder jb = new JsonBuilder();
			// (sb, "data.rate", ClusterServer.dp.getRate());
			jb.pushMap(ClusterServer.cm.getConfiguration().getSettings());
			pw.write(jb.toString());
		} else {
			Debug.println(Debug.ERROR, "Invalid config string passed");
		}

	}

	/**
	 * Runs a query which requires distribution to the cluster.
	 * 
	 * @param query
	 *            A string containing the query to run.
	 * @param pw
	 *            The writer to write the response to.
	 */
	public void runClusterQuery(String query, PrintWriter pw) {
		String type = query.indexOf(' ') == -1 ? query : query.substring(0, query.indexOf(' '));
		String options = query.indexOf(' ') == -1 ? "" : query.substring(query.indexOf(' ') + 1);
		Debug.println(Debug.INFO, "Running a cluster query. type=" + type);

		try {
			switch (type) {
			case "candlestick":
				getCandlestickChartData(options, pw);
				break;
			case "movingAverage":
				getMovingAverageData(options, pw);
				break;
			case "priceToClear":
				priceToClear(options, pw);
				break;
			}
		} catch (SQLException e) {
			Debug.println(Debug.ERROR, "Cluster query exception");
			e.printStackTrace();
		} catch (NoClusterException e) {
			Debug.println(Debug.ERROR, "No cluster");
		}
	}

	private void priceToClear(String options, PrintWriter pw) {
		Pattern p = Pattern.compile("\\s*id\\s*=\\s*(\\d+)\\s*volume\\s*=\\s*(\\d+)\\s*");
		Matcher m = p.matcher(options);
		if (m.find()) {
			String id = m.group(1);
			String volume = m.group(2);
			PriceToClearQuery ptc = new PriceToClearQuery();
			ClusterMaster cm = ClusterServer.cm;
			ptc.stockId = Integer.parseInt(id);
			ptc.volume = Integer.parseInt(volume);
			class PriceToClearCallback extends Callback {
				public PriceToClearResponse ptc = null;

				public void callback(Container data) {
					// TODO Auto-generated method stub
					if (data instanceof PriceToClearResponse) {
						this.ptc = (PriceToClearResponse) data;
						this.finished = true;
					} else {
						Debug.println(Debug.ERROR, "PriceToClear callback run with object of wrong type");
						this.finished = true;
					}
				}
			}
			PriceToClearCallback cb = new PriceToClearCallback();
			try {
				cm.sendPacket(ptc, cb);
			} catch (NoClusterException e) {
				e.printStackTrace();
			}
			cb.waitUntilDone();
			JsonBuilder jb = new JsonBuilder();
			jb.stOb();
			if (null != cb.ptc) {
				jb.pushPair("stockId", cb.ptc.stockId);
				jb.pushPair("volume", cb.ptc.volume);
				jb.pushPair("price", cb.ptc.price);
				jb.pushPair("fullyMet", cb.ptc.fullyMet ? "true" : "false");
			}
			jb.finOb();
			pw.write(jb.toString());

		} else {
			Debug.println(Debug.ERROR, "Invalid priceToClear options");
		}
	}

	private void getCandlestickChartData(String options, final PrintWriter pw) throws SQLException, NoClusterException {
		String[] split = options.split(" ");
		long symbolID = Long.parseLong(split[0]);
		int secondsPerCandlestick = Integer.parseInt(split[1]);

		PreparedStatement rangeStatement = con.prepareStatement("SELECT MIN(offered_s) as min, MAX(offered_s) as max FROM trade WHERE symbol_id=?");
		ResultSet rs;
		rangeStatement.setLong(1, symbolID);
		rs = rangeStatement.executeQuery();
		rs.next();
		long minTime = rs.getLong("min");
		long maxTime = rs.getLong("max");

		rs.close();

		long numberOfCandlesticks = (maxTime - minTime) / secondsPerCandlestick;
		if ((numberOfCandlesticks * secondsPerCandlestick) < (maxTime - minTime))
			numberOfCandlesticks++;
		final long n = numberOfCandlesticks;

		ClusterMaster cm = ClusterServer.cm;

		DistributedQueryCallback c = new DistributedQueryCallback(n) {
			PrintWriter writer = pw;

			protected void processContainer(Container data) {
				CandlestickResponse response = (CandlestickResponse) data;
				writer.write("{");
				writer.write("\"start\":\"" + response.getStart() + "\", ");
				writer.write("\"open\":\"" + response.getOpenValue() + "\", ");
				writer.write("\"close\":\"" + response.getCloseValue() + "\", ");
				writer.write("\"high\":\"" + response.getHighValue() + "\", ");
				writer.write("\"low\":\"" + response.getLowValue() + "\", ");
				writer.write("\"volume\":\"" + response.getVolumeValue() + "\",");
				writer.write("\"time\":\"" + response.getTimeStampS() + "\"");
				writer.write("}");
				if (received != total)
					writer.write(",");
			}
		};

		pw.write("[");
		while (minTime < maxTime) {
			CandlestickRequest request = new CandlestickRequest(symbolID, minTime, secondsPerCandlestick, minTime);
			minTime += secondsPerCandlestick;
			cm.sendPacket(request, c);
		}

		c.waitUntilDone();
		pw.write("]");
	}

	private void getMovingAverageData(String options, final PrintWriter pw) throws SQLException, NoClusterException {
		String[] split = options.split(" ");
		long symbolID = Long.parseLong(split[0]);

		PreparedStatement rangeStatement = con.prepareStatement("SELECT MIN(offered_s) as min, MAX(offered_s) as max FROM trade WHERE symbol_id=?");
		ResultSet rs;
		rangeStatement.setLong(1, symbolID);
		rs = rangeStatement.executeQuery();
		rs.next();
		long minTime = rs.getLong("min");
		long maxTime = rs.getLong("max");
		rs.close();

		pw.write("[");
		getMovingAverageData(minTime, maxTime, 5 * 60, pw, symbolID);
		pw.write(",");
		getMovingAverageData(minTime, maxTime, 10 * 60, pw, symbolID);
		pw.write(",");
		getMovingAverageData(minTime, maxTime, 20 * 60, pw, symbolID);
		pw.write("]");
	}

	private void getMovingAverageData(long minTime, long maxTime, int secondsPerAverage, final PrintWriter pw, long symbolID) throws NoClusterException {
		ClusterMaster cm = ClusterServer.cm;
		final long numberOfQueries = Math.min((maxTime - minTime) - secondsPerAverage, cm.getClientCount() * 2);

		DistributedQueryCallback c = new DistributedQueryCallback(numberOfQueries) {
			PrintWriter writer = pw;

			protected void processContainer(Container data) {
				MovingAverageResponse response = (MovingAverageResponse) data;
				if (received > 1 && response.getAverageCount() > 0)
					writer.write(",");
				for (int i = 0; i < response.getAverageCount(); i++) {
					writer.write("{");
					writer.write("\"time\":\"" + response.getTime(i) + "\", ");
					writer.write("\"average\":\"" + response.getAverage(i) + "\"");
					writer.write("}");
					if (i < response.getAverageCount() - 1)
						writer.write(",");
				}
			}
		};

		pw.write("{\"name\":\"" + (secondsPerAverage / 60) + " minutes\", \"data\":[");

		long length = maxTime - minTime + 1;
		long numberOfAverages = length - secondsPerAverage + 1;
		long averagesPerQuery = numberOfAverages / numberOfQueries;

		long currentStart = minTime;
		for (int i = 0; i < numberOfQueries - 1; i++) {
			MovingAverageRequest request = new MovingAverageRequest(symbolID, currentStart, secondsPerAverage + (averagesPerQuery - 1), secondsPerAverage);
			cm.sendPacket(request, c);
			currentStart += averagesPerQuery;
		}
		MovingAverageRequest request = new MovingAverageRequest(symbolID, currentStart, (maxTime - currentStart) + 1, secondsPerAverage);
		cm.sendPacket(request, c);

		c.waitUntilDone();
		pw.write("]}");
	}

	/**
	 * Runs a basic query. Executes the SQL query string received from the
	 * webserver The result of the query is written to the webserver socket as a
	 * JSON string
	 * 
	 * @param query
	 *            The SQL query to run
	 * @param pw
	 *            PrintWriter to the webserver socket
	 */
	public void runBasicQuery(String query, PrintWriter pw) {
		try {
			Debug.println(100,"Got query: " + query);
			Statement s = con.createStatement();
			ResultSet res = s.executeQuery(query);
			if (!res.isBeforeFirst()) {
				// Result set was empty, return an empty array
				pw.print("[]");
				return;
			}
			String jsonResults = toJSON(res);
			Debug.println(Debug.DEBUG, "Writing: " + jsonResults);
			pw.print(jsonResults);
		} catch (SQLException e) {
			Debug.println(Debug.ERROR, "SQL query exception");
			e.printStackTrace();
			pw.print("SQL Query Exception");
		}
	}

	private void runNotificationsQuery(String query, PrintWriter pw) {
		query = query.trim();
		if (query.equals("")) {
			// Need to get all notifications in the queue.
			pw.write(notifications.getNotificationsJson());
		} else {
			long lastCheck = Long.parseLong(query);
			pw.write(notifications.getNotificationsJson(lastCheck));
		}
	}

	private String toJSON(ResultSet r) throws SQLException {
		JsonBuilder jb = new JsonBuilder();
		ResultSetMetaData rsmd = r.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] columnNames = new String[columnCount];

		for (int i = 1; i <= columnCount; i++) {
			columnNames[i - 1] = rsmd.getColumnName(i);
		}

		jb.stArr();
		while (r.next()) {
			jb.stOb();
			for (int i = 0; i < columnNames.length; i++)
				jb.pushPair(columnNames[i], r.getString(i + 1));
			jb.finOb();
		}
		jb.finArr();
		return jb.toString();
	}

	/**
	 * Converts a ResultSet object to a JSON string Example: [{name: "scott",
	 * age: 20},{name: "greg", age: 19}]
	 * 
	 * @param r
	 *            The ResultSet to convert
	 * @return The JSON String
	 */
	/*
	 * private String toJSON(ResultSet r) throws SQLException {
	 * ResultSetMetaData rsmd = r.getMetaData(); int columnCount =
	 * rsmd.getColumnCount(); String[] columnNames = new String[columnCount];
	 * 
	 * for (int i = 1; i <= columnCount; i++) { columnNames[i - 1] =
	 * rsmd.getColumnName(i); }
	 * 
	 * ArrayList<ArrayList<String>> results = new
	 * ArrayList<ArrayList<String>>();
	 * 
	 * while (r.next()) { ArrayList<String> a = new ArrayList<String>(); for
	 * (int i = 1; i <= columnCount; i++) { a.add(r.getString(i)); }
	 * results.add(a); }
	 * 
	 * String result = "";
	 * 
	 * for (int i = 0; i < results.size(); i++) { String rowJSON =
	 * singleRowToJSON(results.get(i), columnNames); result += rowJSON + ","; }
	 * 
	 * return "[" + result.substring(0, result.length() - 1) + "]"; }
	 */

	/**
	 * Converts a single result row to JSON string Example: row=["scott", 20,
	 * "Christ's"], columnNames=["Name", "Age", "College"] Output:
	 * {"name":"scott", "age":"20", "college":"Christ's"}
	 * 
	 * @param row
	 *            The row data
	 * @param columnNames
	 *            Table column names
	 * @return A JSON string for the row
	 */
	/*
	 * private String singleRowToJSON(ArrayList<String> row, String[]
	 * columnNames) { String result = ""; for (int i = 0; i < row.size(); i++) {
	 * result += "\"" + columnNames[i] + "\": " + "\"" + row.get(i) + "\"" +
	 * ","; }
	 * 
	 * return "{" + result.substring(0, result.length() - 1) + "}"; }
	 */
}
