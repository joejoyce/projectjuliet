package uk.ac.cam.cl.juliet.test.debug;

import uk.ac.cam.cl.juliet.common.Debug;

public class DebugServerTest {

	public static void main(String args[]) {
		Debug.registerOutputLocation(System.out);
		Debug.setPriority(10);
		Debug.println("HI");
		Debug.setPriority(5);
		Debug.println("LO");
		Debug.println(5,"LO5");
		Debug.println(2,"LO2");
		Debug.println(10,"LO10");
	}
}
