package uk.ac.cam.cl.juliet.common;

/**
 * The response of a moving average request.
 * 
 * @author Dylan McDermott
 * 
 */
public class MovingAverageResponse extends QueryResponse {
	private static final long serialVersionUID = 1L;
	private long[] times;
	private double[] values;

	/**
	 * Creates a new moving average response
	 * 
	 * @param id
	 *            The id of the request
	 * @param times
	 *            An array of the times in seconds which the averages correspond
	 *            to
	 * @param values
	 *            An array of average stock prices
	 */
	public MovingAverageResponse(long id, long[] times, double[] values) {
		super(id, true);
		this.times = times;
		this.values = values;
	}

	public int getAverageCount() {
		return times.length;
	}

	public long getTime(int index) {
		return times[index];
	}

	public double getAverage(int index) {
		return values[index];
	}
}
