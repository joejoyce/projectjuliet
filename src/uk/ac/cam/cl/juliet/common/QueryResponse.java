package uk.ac.cam.cl.juliet.common;

/**
 * The response of a query.
 * 
 * @author Dylan McDermott
 * 
 */
public class QueryResponse extends Container {
	private static final long serialVersionUID = 1L;
	private boolean result;

	public QueryResponse(long id, boolean result) {
		this.setPacketId(id);
		this.result = result;
	}

	/**
	 * Gets the result of the query.
	 * @returns true if the query was completed successfully, otherwise false.
	 */
	public boolean getResult() {
		return result;
	}


	/**
	 * Sets the result of processing the query.
	 * @param value The result.
	 */
	public void setResult(boolean value) {
		this.result = value;
	}
}
