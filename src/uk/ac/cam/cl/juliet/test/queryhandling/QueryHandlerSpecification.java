package uk.ac.cam.cl.juliet.test.queryhandling;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;

import uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling.WebServerListener;

public class QueryHandlerSpecification {
	public static void runTests() throws Exception {
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/juliettest", "root", "rootword");
		con.createStatement().execute("DROP TABLE IF EXISTS symbol");
		con.createStatement().execute("CREATE TABLE IF NOT EXISTS symbol (symbol_id int(10) unsigned NOT NULL,  symbol varchar(16) NOT NULL,  company_name varchar(100) NOT NULL,  price_scale int(10) unsigned NOT NULL,  open_price int(10) unsigned NOT NULL,  PRIMARY KEY(symbol_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0");
		con.createStatement().execute("INSERT INTO symbol VALUES (9,'AAPL','Apple. Inc',4,0)");
		
		con.createStatement().execute("DROP TABLE IF EXISTS trade");
		con.createStatement().execute("CREATE TABLE IF NOT EXISTS trade (id int(10) unsigned NOT NULL AUTO_INCREMENT,trade_id int(10) unsigned NOT NULL,  symbol_id int(10) unsigned NOT NULL,  price int(10) unsigned NOT NULL,  volume int(10) unsigned NOT NULL,  offered_s int(10) unsigned NOT NULL,  offered_seq_num int(10) unsigned NOT NULL,  completed_s int(10) unsigned NULL,  completed_seq_num int(10) unsigned NULL,  PRIMARY KEY(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=0");
		con.createStatement().execute("INSERT INTO trade VALUES (1,1,9,400,1000,0,0,0,0)");
		
		new WebServerListener(1337, con);
		
		Socket s = new Socket("127.0.0.1", 1337);
		BufferedReader din  = new BufferedReader(new InputStreamReader(new BufferedInputStream(s.getInputStream())));
		PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
		
		pw.println("basic|SELECT * from symbol WHERE symbol='AAPL'");
		String res = din.readLine();		
		assert(res == "[{\"symbol_id\": \"9\",\"symbol\": \"AAPL\",\"company_name\": \"Apple. Inc\",\"price_scale\": \"4\",\"open_price\": \"0\"}]");
		
		s.close();
		
		
		s = new Socket("127.0.0.1", 1337);
		din  = new BufferedReader(new InputStreamReader(new BufferedInputStream(s.getInputStream())));
		pw = new PrintWriter(s.getOutputStream(), true);
		
		pw.println("basic|SELECT * from trade WHERE symbol_id=9");
		res = din.readLine();
		assert(res == "[{\"id\": \"1\",\"trade_id\": \"1\",\"symbol_id\": \"9\",\"price\": \"400\",\"volume\": \"1000\",\"offered_s\": \"0\",\"offered_seq_num\": \"0\",\"completed_s\": \"0\",\"completed_seq_num\": \"0\"}]");
		
		s.close();
	}
	
	public static void main(String[] args) {
		try {
			QueryHandlerSpecification.runTests();
		} catch (Exception e) {
			System.out.println("[QueryHandler] Tests failed");
			e.printStackTrace();
			return;
		}
		
		System.out.println("[QueryHandler] All tests passed!");
		System.exit(0);
	}
}
