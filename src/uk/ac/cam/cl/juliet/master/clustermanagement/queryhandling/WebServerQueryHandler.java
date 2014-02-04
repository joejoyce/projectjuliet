package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import uk.ac.cam.cl.juliet.common.QueryPacket;
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
				case "basic": runBasicQuery(splitQuery[1], pw);
							  break;
				default: System.out.println("Unsupported query type");
			}
			
			pw.flush();
			pw.close();
			server.close();
		} catch (IOException e) {
			System.out.println("Error reading query from webserver");
			e.printStackTrace();
		}
    }
	
	public void runQuery(QueryPacket p, int id) {
    	
    }
	
	public void runBasicQuery(String query, PrintWriter pw) {
		try {			
			Statement s = con.createStatement();
			ResultSet res = s.executeQuery(query);
			String jsonResults = toJSON(res);
			pw.print(jsonResults);
		} catch(SQLException e) {
			System.out.println("SQL query exception");
			e.printStackTrace();
			pw.print("SQL Query Exception");
		}
	}
	
	// {results: [{name: "scott", age: 20},{name: "greg", age: 19}]}
	private String toJSON(ResultSet r) throws SQLException {
		ResultSetMetaData rsmd = r.getMetaData();
		int columnCount = rsmd.getColumnCount();
		String[] columnNames = new String[columnCount];
		
		for(int i = 1; i <= columnCount; i ++) {
			columnNames[i-1] = rsmd.getColumnName(i);
		}
		
		//String[][] results = new String[rowCount][columnCount];
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
	
	private String singleRowToJSON(ArrayList<String> row, String[] columnNames) {
		String result = "";
		for(int i = 0; i < row.size(); i ++) {
			result += "\"" + columnNames[i] + "\": " + "\"" + row.get(i) + "\"" + ",";
		}
		
		return "{" + result.substring(0, result.length() - 1) + "}";
	}	
	
}
