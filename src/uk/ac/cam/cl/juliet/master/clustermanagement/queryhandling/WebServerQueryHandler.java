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
import java.util.Arrays;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.Client;

import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.QueryPacket;
import uk.ac.cam.cl.juliet.master.ClusterServer;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
/**
 * @description WebServerQueryHandler
 * Extracts a query from a WebServer connection socket and 
 * decides what to do. This may involve sending the QueryPacket
 * off to the Distributor. 
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
			BufferedReader din  = new BufferedReader(new InputStreamReader(new BufferedInputStream(server.getInputStream())));
			PrintWriter pw = new PrintWriter(server.getOutputStream(), true);
			
			String query = din.readLine();			
			String[] splitQuery = query.split("\\|");		
			
			switch(splitQuery[0]) {
				case "basic": 	runBasicQuery(splitQuery[1], pw);
							  	break;
				case "status": 	runStatusQuery(splitQuery[1], pw);
							   	break;
				case "config":	runConfigQuery(splitQuery[1], pw);
							   	break;
				case "cluster": runClusterQuery(splitQuery[1], pw);
								break;
				default: Debug.println(Debug.ERROR,"Unsupported query type");
			}
			
			pw.flush();
			pw.close();
			server.close();
		} catch (IOException e) {
			Debug.println(Debug.ERROR,"Error reading query from webserver");
			e.printStackTrace();
		} 
    }
	
	public void runQuery(QueryPacket p, int id) {}
	
	public void runStatusQuery(String query, PrintWriter pw) {
		//Returns a json array of objects of name, totalPackets and currentPackets
		if(query.equals("listclients")) {
			Debug.println(Debug.INFO,"Running a status query");
			ClusterMaster cm = ClusterServer.cm;
			Client carr[] = cm.listClients();
			StringBuilder res = new StringBuilder();
			res.append("[");
			for(int i = 0; i< carr.length; i++) {
				res.append(" { \"name\" : \"");
				res.append(carr[i].getClientIP().toString());
				res.append("\", \"totalPackets\" : ");
				res.append(carr[i].getTotalWork());
				res.append(", \"currentPackets\" : ");
				res.append(carr[i].getCurrentWork());
				res.append("}");
				if(i != (carr.length - 1)) {
					res.append(",");
				}
			}
			res.append("]");
			String rtn = res.toString();
			Debug.println(Debug.DEBUG,"Status query result" + rtn);
			pw.print(rtn);
		}
	}
	public void runConfigQuery(String query, PrintWriter pw) {}
	public void runClusterQuery(String query, PrintWriter pw) {
		String type = query.indexOf(' ') == -1 ? query : query.substring(0, query.indexOf(' '));
		String options = query.indexOf(' ') == -1? "" : query.substring(query.indexOf(' ') + 1);
		Debug.println(Debug.INFO,"Running a cluster query. type=" + type);
		
		switch(type) {
		case "candlestick":
			getCandlestickChartData(options, pw);
		}
	}
	
	private void getCandlestickChartData(String options, PrintWriter pw) throws SQLException {
		String[] split = options.split(" ");
		long symbolID = Long.parseLong(split[0]);
		int secondsPerCandlestick = Integer.parseInt(split[1]);
		
	    PreparedStatement rangeStatement = con.prepareStatement("SELECT MIN(offered_s) as min, MAX(offered_s) as max FROM trade WHERE symbol_id=?");
	    ResultSet rs;
	    try {
	    	rangeStatement.setLong(1, symbolID);
	    	rs = rangeStatement.executeQuery();
	    }finally{
	    	rangeStatement.close();
	    }
	    rs.next();
	    long minTime = rs.getLong("min");
	    long maxTime = rs.getLong("max");
	    
	    long numberOfCandlesticks = (maxTime - minTime) / secondsPerCandlestick;
	    
	}

	/**
	 * Runs a basic query.
	 * Executes the SQL query string received from the webserver
	 * The result of the query is written to the webserver socket
	 * as a JSON string
	 * @param query	The SQL query to run
	 * @param pw PrintWriter to the webserver socket
	 */
	public void runBasicQuery(String query, PrintWriter pw) {
		try {		
			Debug.println(Debug.INFO,"Got query: " + query);
			Statement s = con.createStatement();
			ResultSet res = s.executeQuery(query);
			if(!res.isBeforeFirst()) {    
				// Result set was empty, return an empty array
				pw.print("[]");
				return;
			}
			String jsonResults = toJSON(res);
			Debug.println(Debug.DEBUG,"Writing: " + jsonResults);
			pw.print(jsonResults);
		} catch(SQLException e) {
			Debug.println(Debug.ERROR,"SQL query exception");
			e.printStackTrace();
			pw.print("SQL Query Exception");
		}
	}
	
	/**
	 * Converts a ResultSet object to a JSON string
	 * Example: [{name: "scott", age: 20},{name: "greg", age: 19}]
	 * @param r	The ResultSet to convert
	 * @return The JSON String
	 */
	private String toJSON(ResultSet r) throws SQLException {
		ResultSetMetaData rsmd = r.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] columnNames = new String[columnCount];
		
		for(int i = 1; i <= columnCount; i ++) {
			columnNames[i-1] = rsmd.getColumnName(i);
		}
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		
		while(r.next()) {
			ArrayList<String> a = new ArrayList<String>();
			for(int i = 1; i <= columnCount; i ++) {
				a.add(r.getString(i));
			}
			results.add(a);
		}		
		
		String result = "";
		
		for(int i = 0; i < results.size(); i ++) {
			String rowJSON = singleRowToJSON(results.get(i), columnNames);
			result += rowJSON + ",";
		}
		
		return "[" + result.substring(0, result.length() - 1) + "]";
	}
	
	/**
	 * Converts a single result row to JSON string
	 * Example: row=["scott", 20, "Christ's"], columnNames=["Name", "Age", "College"]
	 * Output: {"name":"scott", "age":"20", "college":"Christ's"} 
	 * 
	 * @param row The row data
	 * @param columnNames Table column names
	 * @return A JSON string for the row
	 */
	private String singleRowToJSON(ArrayList<String> row, String[] columnNames) {
		String result = "";
		for(int i = 0; i < row.size(); i ++) {
			result += "\"" + columnNames[i] + "\": " + "\"" + row.get(i) + "\"" + ",";
		}
		
		return "{" + result.substring(0, result.length() - 1) + "}";
	}		
}
