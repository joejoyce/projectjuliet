package uk.ac.cam.cl.juliet.master;

import java.io.IOException;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMasterUnit;
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
	public static DataProcessor dp;
	
	@SuppressWarnings("unused")
	public static void main(String args[]) throws IOException, SQLException {
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/juliet", "root", "rootword");
        WebServerListener wsl = new WebServerListener(1337, con);
        DatabaseCleaner c = new DatabaseCleaner(con);

		Debug.registerOutputLocation(System.out);
        Debug.setPriority(10);

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
        cm = new ClusterMasterUnit("settings");
        cm.start(5000);
        final DataProcessor dp = new DataProcessor(ds, cm);
        ClusterServer.dp = dp;
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
			} else if (Debug.parseDebugArgs(input)) {
				continue;
			} else {
				try {
					float skip = Float.parseFloat(input);
					ds.skipBoundary = (long) (1000000000*skip);
				} catch(NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		s.close();
		System.exit(0);
	}
}
