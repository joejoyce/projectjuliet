package uk.ac.cam.cl.juliet.test.clustermanagement.distribution;

import uk.ac.cam.cl.juliet.common.XDPResponse;
/**
 * A Mock XDPResponse that is send by a MockPi and contains the sequence number
 * of the packet that arrived at the Pi.
 * 
 * @author Lucas Sonnabend
 *
 */
public class MockXDPResponse extends XDPResponse {
	private static final long serialVersionUID = 1L;
	private int sequenceNo;
	
	public MockXDPResponse(long id, boolean result) {
		super(id, result);
		sequenceNo = -1;
	}
	/**
	 * create a new MockXDPRespsonse and pass it on the sequence number of the 
	 * packet the MockPi received
	 * @param id
	 * @param result
	 * @param seqNo
	 */
	public MockXDPResponse(long id, boolean result, int seqNo) {
		super(id, result);
		sequenceNo = seqNo;
	}
	/**
	 * returns the sequence number of the packet that triggered this response
	 * object in a MockPi
	 * @return
	 */
	public int getSequenceNo() {return this.sequenceNo;}

}
