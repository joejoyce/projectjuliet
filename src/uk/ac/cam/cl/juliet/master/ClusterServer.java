package uk.ac.cam.cl.juliet.master;

import java.io.IOException;

import uk.ac.cam.cl.juliet.common.StringTestPacket;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.ClusterMaster;
import uk.ac.cam.cl.juliet.master.clustermanagement.distribution.NoClusterException;

/**
 * Prepare the ClusterServer for operation - also run some tests?
 * @author joseph
 *
 */
public class ClusterServer {
	
	public static void main(String args[]) throws IOException {
		ClusterMaster m = new ClusterMaster("");
		m.start(5000);
		try {
			m.sendPacket(new StringTestPacket("Is this working?"));
		} catch (NoClusterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Run all tests: - include delay to bring Raspberry Pis up?
		
		System.in.read();
		m.stop();
	}
}