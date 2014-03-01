package uk.ac.cam.cl.juliet.master.dataprocessor;

import java.io.IOException;

import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.common.XDPRequest;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
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
	private String f1, f2, f3, f4;
	private float skip;
	
	public DataProcessor(XDPDataStream dataStream, ClusterMaster cm) {
		this.dataStream = dataStream;
		this.clusterMaster = cm;
	}
	
	public void setFiles(String f1, String f2, String f3, String f4, float skip) {
		this.f1 = f1;
		this.f2 = f2;
		this.f3 = f3;
		this.f4 = f4;
		this.skip = skip;
	}
	
	public XDPDataStream getDataStream() {
		return this.dataStream;
	}

	public void start() {
		XDPRequest packet = null;
		do {
			try {
				while(pause) {
					Thread.currentThread().sleep(100);
				}
				packet = dataStream.getPacket();
				if(packet.getDeliveryFlag() == 11) {
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
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (packet != null);
		Debug.println(100, "Finished entire stream");
	}
	
	public void restartAfresh() {
		try {
			// Very harsh, but should work
			this.dataStream = new SampleXDPDataStream(f1, f2, f3, f4, skip);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
