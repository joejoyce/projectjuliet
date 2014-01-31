package uk.ac.cam.cl.juliet.master.dataprocessor;

import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;

import java.io.IOException;

public class DataProcessor {
	private XDPDataStream dataStream;
	private ClusterMaster clusterMaster;
	
	public DataProcessor(XDPDataStream dataStream, ClusterMaster cm) {
		this.dataStream = dataStream;
		this.clusterMaster = cm;
	}

	public void start() {
		XDPRequest packet = null;
		do {
			try {
				packet = dataStream.getPacket();
				//if(packet.getDeliveryFlag() == 11)
					//clusterMaster.sendPacket(packet);				
			} catch (IOException e) {
				System.err.println("Datastream error");
				e.printStackTrace();
				break;
			} /*catch (NoClusterException e) {
				System.err.println("Cluster error");
				e.printStackTrace();
				break;
			}*/
		} while (packet != null);
	}

	public static void main(String[] args) throws IOException {
		SampleXDPDataStream ds = new SampleXDPDataStream();
		ClusterMaster cm = new ClusterMaster();
		DataProcessor dp = new DataProcessor(ds, cm);
		dp.start();
	}
}