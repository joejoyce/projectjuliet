package uk.ac.cam.cl.juliet.test.dataprocessor;

import java.io.IOException;

import uk.ac.cam.cl.juliet.master.dataprocessor.DataProcessor;
import uk.ac.cam.cl.juliet.master.dataprocessor.SampleXDPDataStream;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;

/**
 * This class tests the data processor
 * 
 * @author michael
 */
public class DataProcessorTest {

	public static void main(String[] args) {
		// Take input data files and skip boundary
		String file1, file2, file3, file4;
		float skipBoundary;
		try {
			file1 = args[0];
			file2 = args[1];
			file3 = args[2];
			file4 = args[3];
			skipBoundary = Float.parseFloat(args[4]);
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.err.println("Usage: <file1> <file2> <file3> <file4> <skipBoundary>");
			return;
		}
		
		try {
			// Create data stream and cluster master
			XDPDataStream dataStream = new SampleXDPDataStream(
					file1, file2, file3, file4, skipBoundary
			);
			MockClusterMaster clusterMaster = new MockClusterMaster();
			
			clusterMaster.start(5000);
			
			// Create data processor and start test
			DataProcessor dataProcessor = new DataProcessor(dataStream, clusterMaster);
			System.out.println("Starting test");
			dataProcessor.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
