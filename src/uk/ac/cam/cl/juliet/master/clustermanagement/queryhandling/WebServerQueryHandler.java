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
			String[] splitQuery = query.split("|");
			
			
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
			pw.print("SQL Query Exception");
		}
	}
	
	// {results: [{name: "scott", age: 20},{name: "greg", age: 19}]}
	private String toJSON(ResultSet r) throws SQLException {
		ResultSetMetaData rsmd = r.getMetaData();
		int columnCount = rsmd.getColumnCount();
		int rowCount = r.getRow();
		String[] columnNames = new String[columnCount];
		
		for(int i = 0; i < columnCount; i ++) {
			columnNames[i] = rsmd.getColumnName(i);
		}
		
		String[][] results = new String[rowCount][columnCount];
		
		int row = 0;
		
		while(r.next()) {
			for(int i = 0; i < columnCount; i ++) {
				results[row++][i] = r.getString(i);
			}			
		}		
		
		String result = "";
		
		for(int i = 0; i < results.length; i ++) {
			String rowJSON = singleRowToJSON(results[i], columnNames);
			result += rowJSON + ",";
		}
		
		return "[" + result.substring(0, result.length() - 1) + "]";
	}
	
	private String singleRowToJSON(String[] row, String[] columnNames) {
		String result = "";
		for(int i = 0; i < row.length; i ++) {
			result += columnNames[i] + ": " + "\"" + row[i] + "\"" + ",";
		}
		
		return "{" + result.substring(0, result.length() - 1) + "}";
	}	
	
}
