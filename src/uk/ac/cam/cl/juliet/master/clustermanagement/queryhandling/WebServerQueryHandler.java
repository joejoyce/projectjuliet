package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import uk.ac.cam.cl.juliet.common.CandlestickRequest;
import uk.ac.cam.cl.juliet.common.CandlestickResponse;
import uk.ac.cam.cl.juliet.common.Container;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.MovingAverageRequest;
import uk.ac.cam.cl.juliet.common.MovingAverageResponse;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.master.ClusterServer;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Callback;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;

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

	public WebServerQueryHandler(Socket server, Connection con) {
		this.server = server;
		this.con = con;
	}

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

	public void runQuery(QueryPacket p, int id) {
	}

	public void runStatusQuery(String query, PrintWriter pw) {
		// Returns a json array of objects of name, totalPackets and
		// currentPackets
		if (query.equals("listclients")) {
			Debug.println(Debug.INFO, "Running a status query");
			ClusterMaster cm = ClusterServer.cm;
			Client carr[] = cm.listClients();
			StringBuilder res = new StringBuilder();
			res.append("[");
			for (int i = 0; i < carr.length; i++) {
				res.append(" { \"name\" : \"");
				res.append(carr[i].getClientIP().toString());
				res.append("\", \"totalPackets\" : ");
				res.append(carr[i].getTotalWork());
				res.append(", \"currentPackets\" : ");
				res.append(carr[i].getCurrentWork());
				res.append("}");
				if (i != (carr.length - 1)) {
					res.append(",");
				}
			}
			res.append("]");
			String rtn = res.toString();
			Debug.println(Debug.DEBUG, "Status query result" + rtn);
			pw.print(rtn);
		}

		if (query.equals("time")) {
			ClusterMaster cm = ClusterServer.cm;
			long t = cm.getTime();
			pw.write(t + "");
		}

		if (query.equals("throughput")) {
			ClusterMaster cm = ClusterServer.cm;
			int total = cm.getPacketThroughput();
			pw.write(total + "");
		}
	}

	public void runConfigQuery(String query, PrintWriter pw) {
	}

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
			}
		} catch (SQLException e) {
			Debug.println(Debug.ERROR, "Cluster query exception");
			e.printStackTrace();
		} catch (NoClusterException e) {
			Debug.println(Debug.ERROR, "No cluster");
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
				writer.write("\"volume\":\"" + response.getVolumeValue() + "\"");
				writer.write("}");
				if (received != total)
					writer.write(",");
			}
		};

		pw.write("[");
		while (minTime < maxTime) {
			CandlestickRequest request = new CandlestickRequest(symbolID, minTime, secondsPerCandlestick);
			minTime += secondsPerCandlestick;
			cm.sendPacket(request, c);
		}

		c.waitUntilDone();
		pw.write("]");
	}

	private void getMovingAverageData(String options, final PrintWriter pw) throws SQLException, NoClusterException {
		String[] split = options.split(" ");
		long symbolID = Long.parseLong(split[0]);
		int secondsPerAverage = Integer.parseInt(split[1]);

		PreparedStatement rangeStatement = con.prepareStatement("SELECT MIN(offered_s) as min, MAX(offered_s) as max FROM trade WHERE symbol_id=?");
		ResultSet rs;
		rangeStatement.setLong(1, symbolID);
		rs = rangeStatement.executeQuery();
		rs.next();
		long minTime = rs.getLong("min");
		long maxTime = rs.getLong("max");
		rs.close();
		ClusterMaster cm = ClusterServer.cm;
		final long numberOfQueries = Math.min((maxTime - minTime) - secondsPerAverage, cm.getClientCount() * 2);

		DistributedQueryCallback c = new DistributedQueryCallback(numberOfQueries) {
			PrintWriter writer = pw;

			protected void processContainer(Container data) {
				MovingAverageResponse response = (MovingAverageResponse) data;
				for (int i = 0; i < response.getAverageCount(); i++) {
					writer.write("{");
					writer.write("\"time\":\"" + response.getTime(i) + "\"");
					writer.write("\"average\":\"" + response.getAverage(i) + "\"");
					writer.write("}");
					if (received != total && i < response.getAverageCount() - 1)
						writer.write(",");
				}
			}
		};

		pw.write("[");

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
		pw.write("]");
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
			Debug.println(Debug.INFO, "Got query: " + query);
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

	/**
	 * Converts a ResultSet object to a JSON string Example: [{name: "scott",
	 * age: 20},{name: "greg", age: 19}]
	 * 
	 * @param r
	 *            The ResultSet to convert
	 * @return The JSON String
	 */
	private String toJSON(ResultSet r) throws SQLException {
		ResultSetMetaData rsmd = r.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] columnNames = new String[columnCount];

		for (int i = 1; i <= columnCount; i++) {
			columnNames[i - 1] = rsmd.getColumnName(i);
		}

		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();

		while (r.next()) {
			ArrayList<String> a = new ArrayList<String>();
			for (int i = 1; i <= columnCount; i++) {
				a.add(r.getString(i));
			}
			results.add(a);
		}

		String result = "";

		for (int i = 0; i < results.size(); i++) {
			String rowJSON = singleRowToJSON(results.get(i), columnNames);
			result += rowJSON + ",";
		}

		return "[" + result.substring(0, result.length() - 1) + "]";
	}

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
	private String singleRowToJSON(ArrayList<String> row, String[] columnNames) {
		String result = "";
		for (int i = 0; i < row.size(); i++) {
			result += "\"" + columnNames[i] + "\": " + "\"" + row.get(i) + "\"" + ",";
		}

		return "{" + result.substring(0, result.length() - 1) + "}";
	}
}
