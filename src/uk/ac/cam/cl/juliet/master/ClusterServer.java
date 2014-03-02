package uk.ac.cam.cl.juliet.master;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMasterUnit;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.master.dataprocessor.DataProcessor;
import uk.ac.cam.cl.juliet.master.dataprocessor.SampleXDPDataStream;
import uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling.SpikeDetectionRunnable;
import uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling.WebServerListener;


/**
 * @description Prepare the ClusterServer for operation
 * Contains the entry point for the system
 *  
 * @author Joseph
 * 
 */
public class ClusterServer {
	public static ClusterMaster cm;
	public static DataProcessor dp;
	public static SpikeDetectionRunnable spikeDetector;
	public static ShutdownSettingsSaver settingsSaver;
	
	public static final String SETTINGS_FILE = "settings";
	private static boolean USE_INPUT_FILES_FROM_SETTING = false;

	@SuppressWarnings("unused")
	public static void main(String args[]) throws IOException, SQLException, InterruptedException {
		Debug.registerOutputLocation(System.out);
		Debug.setPriority(Debug.INFO);
		
		Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/juliet", "root", "rootword");
		WebServerListener wsl = new WebServerListener(1337, con);
		DatabaseCleaner c = new DatabaseCleaner(con);

		String[] files = new String[4];
		float skipBoundary = 0;
		try {
			if (args.length > 4) {
				files[0] = args[0];
				files[1] = args[1];
				files[2] = args[2];
				files[3] = args[3];
				skipBoundary = Float.parseFloat(args[4]);
			} else if (args.length > 0) {
				skipBoundary = Float.parseFloat(args[0]);
				USE_INPUT_FILES_FROM_SETTING = true;
			} else {
				System.err.println("Usage: <file1> <files2> <file3> <file4> <skipBoundary> or\n Usage: <skipBoundary>");
				return;
			}
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("Usage: <file1> <files2> <file3> <file4> <skipBoundary> or\n Usage: <skipBoundary>");
			return;
		}

		// Read in settings file
		Map<String, String> clusterMasterSettings = new HashMap<String, String>();
		long[] dataStreamPositions = new long[4];
		FileReader fr = new FileReader(SETTINGS_FILE);
		BufferedReader bf = new BufferedReader(fr);
		try {
			String line = null;
			int i = 0;
			while (null != (line = bf.readLine())) {
				String arr[] = line.split(" ");
				if (arr.length > 2) {
					if (arr[0].equals("client")) {
						clusterMasterSettings.put(arr[1], arr[2]);
					}
					if (USE_INPUT_FILES_FROM_SETTING && arr[0].equals("datastream")) {
						files[i] = arr[1];
						dataStreamPositions[i] = Long.parseLong(arr[2]);
						i++;
					}
				}
			}
			bf.close();
		} catch (IOException e) {
			Debug.print(Debug.ERROR, "could not read in settings file " + SETTINGS_FILE + ".");
			e.printStackTrace();
		}
		
		// Create new thread to save settings when exiting JVM
		settingsSaver = new ShutdownSettingsSaver(SETTINGS_FILE);

		cm = new ClusterMasterUnit(clusterMasterSettings, settingsSaver);
		cm.start(5000);
		
		// Create an XDPDataStream, use the settings if they are available
		SampleXDPDataStream ds;
		if (USE_INPUT_FILES_FROM_SETTING) {
			if (files[3] != null && files[2] != null && files[1] != null && files[0] != null) {
				ds = new SampleXDPDataStream(files[0], dataStreamPositions[0], files[1], dataStreamPositions[1], files[2], dataStreamPositions[2], files[3], dataStreamPositions[3], skipBoundary);
			} else {
				// If the settings file does not specify enough arguments
				Debug.println(Debug.SHOWSTOP, "Insufficient number of input files! " 
											+ "Either provide the input file names as arguments to the main function " 
											+ "or specify all for in the settings file, including file pointer position " 
											+ "(choose 0 to start at the beginning of the file)");
				return;
			}
		} else {
			ds = new SampleXDPDataStream(files[0], files[1], files[2], files[3], skipBoundary);
		}
		
		final DataProcessor dp = new DataProcessor(ds, cm);
		dp.setFiles(files[0], files[1], files[2], files[3], skipBoundary);
		dp.pause = true;
		ClusterServer.dp = dp;
		settingsSaver.setDataStream(dp);

		// Start the DataProcessor
		Thread t = new Thread() {
			public void run() {
				dp.start();
			}
		};
		t.start();
		
		// Create a new spike detection thread with default values
		//spikeDetector = new SpikeDetectionRunnable(cm, 10, 1800, 0.05f);
		//spikeDetector.setRunning(true);

		// Read command line input		
		Scanner s = new Scanner(System.in);
		String input = "";
		while (!input.equals("quit")) {
			input = s.nextLine();
			System.out.println("input: " + input);
			if (input.equals("pause")) {
				dp.pause = !dp.pause;
			} else if (Debug.parseDebugArgs(input)) {
				continue;
			} else {
				try {
					float skip = Float.parseFloat(input);
					ds.setSkipBoundary(skip);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		s.close();
		dp.pause = true;
		settingsSaver.savePointers();
		System.exit(1);
	}
}
