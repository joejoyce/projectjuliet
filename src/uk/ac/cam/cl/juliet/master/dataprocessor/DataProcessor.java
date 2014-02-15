package uk.ac.cam.cl.juliet.master.dataprocessor;

import java.io.IOException;
import java.util.Scanner;

import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMasterUnit;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;

/**
 * @description This class attaches to a XDPDataStream and requests packets.
 * Only packets with dilveryFlag == 11 are passed on to the Distributor
 * 
 * @author Scott Williams
 */
public class DataProcessor {
	private XDPDataStream dataStream;
	private ClusterMaster clusterMaster;
	public volatile boolean pause = false;
	
	public DataProcessor(XDPDataStream dataStream, ClusterMaster cm) {
		this.dataStream = dataStream;
		this.clusterMaster = cm;
	}

	public void start() {
		XDPRequest packet = null;
		//Scanner scan = new Scanner(System.in);
		do {
			try {
				while(pause){}
				packet = dataStream.getPacket();
				if(packet.getDeliveryFlag() == 11) {
					//scan.nextLine();
					clusterMaster.sendPacket(packet);
				}
			} catch (IOException e) {
				System.err.println("Datastream error");
				e.printStackTrace();
				break;
			} catch (NoClusterException e) {
				System.err.println("Cluster error");
				e.printStackTrace();
				break;
			}
		} while (packet != null);
		Debug.println("Finished entire stream");
	}

	public static void main(String[] args) throws IOException {
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
		ClusterMaster m = new ClusterMasterUnit("");
		m.start(5000);
		DataProcessor dp = new DataProcessor(ds, m);
		Scanner s = new Scanner(System.in);
		Debug.println("GO?");
		s.nextLine();
		dp.start();
		s.close();
	}
}
