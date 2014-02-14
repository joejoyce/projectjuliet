package uk.ac.cam.cl.juliet.master;

import java.io.IOException;

import uk.ac.cam.cl.juliet.common.StringTestPacket;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMasterUnit;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;
import uk.ac.cam.cl.juliet.common.Debug;
import java.util.Scanner;
import uk.ac.cam.cl.juliet.master.dataprocessor.DataProcessor;
import uk.ac.cam.cl.juliet.master.dataprocessor.SampleXDPDataStream;
import uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling.WebServerListener;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Prepare the ClusterServer for operation - also run some tests?
 * @author joseph
 *
 */
public class ClusterServer {

	public static ClusterMaster cm;
	
	public static void main(String args[]) throws IOException, SQLException {
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/juliet", "root", "rootword");
                new WebServerListener(1337, con);

		Debug.registerOutputLocation(System.out);
                Debug.setPriority(10); //Default is 5 so no msg show

                String file1, file2, file3, file4;
                float skipBoundary;
                try {
                        file1 = args[0];
                        file2 = args[1];
                        file3 = args[2];
                        file4 = args[3];
                        skipBoundary = Float.parseFloat(args[4]);
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                        System.err.println("Usage: <file1> <files2> <file3> <file4> <skipBoundary>");
                        return;
                }
                SampleXDPDataStream ds = new SampleXDPDataStream(file1, file2, file3,file4, skipBoundary);
                cm = new ClusterMasterUnit("");
                cm.start(5000);
                final DataProcessor dp = new DataProcessor(ds, cm);
                Scanner s = new Scanner(System.in);
                System.out.println("GO?");
		s.nextLine();
		Thread t = new Thread(){
			public void run() {
				dp.start();
			}
		};
		t.start();

		String input = "";
		while(!input.equals("quit")) {
			input = s.nextLine();
			System.out.println("input: " + input);
			if(input.equals("pause")) {
				dp.pause = !dp.pause;
			} else if(input.matches("debug:\\s*\\w*")) {
				String dargs[] = input.split(":");
				if(dargs.length > 1) {
					switch(dargs[1].trim()) {
						case "ALL":
							Debug.setPriority(Debug.ALL);
							break;
						case "DEBUG":
							Debug.setPriority(Debug.DEBUG);
							break;
						case "INFO":
							Debug.setPriority(Debug.INFO);
							break;
						case "WARN":
							Debug.setPriority(Debug.WARN);
							break;
						case "ERROR":
							Debug.setPriority(Debug.ERROR);
							break;
						case "SHOWSTOP":
							Debug.setPriority(Debug.SHOWSTOP);
							break;
						default :
							System.err.println(dargs[1] + " is not a valid debug level");
					}
				} else
					System.err.println("Malformed debug level expression");
			}
			
		}
		System.exit(0);
	}
}
