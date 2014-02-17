package uk.ac.cam.cl.juliet.common;
/**
 * A request that is send to a Pi that shall run a spike detection, possibly 
 * on all stocks
 * @author lucas
 *
 */
public class SpikeDetectionRequest extends QueryPacket {
	private static final long serialVersionUID = 1L;
	private long startTimeAverage;
	private long startTimeSpikeDetection;
	private float limit;

	/**
	 * create a new SpikeDetectionRequest
	 * @param pStartTimeAverage
	 * 				determines the time frame for taking the average for the spike 
	 * 				detection. The time frame is from pStartTimeAverage until the 
	 * 				most recent entry in the database
	 * @param pStartTimeSkpikeDetection
	 * 				determines the time frame in which you search for spikes.
	 * 				This time frame is from pStartTimeSpikeDetection to the most
	 * 				recent entry in the database
	 * @param pLimit
	 * 				determines in percent by how much a trade has to exceed the
	 * 				average to be considered a spike. For example if limit = 0.05, 
	 * 				then a trade has to be 5% higher or lower than the average to 
	 * 				be a spike.
	 */
	public SpikeDetectionRequest(long pStartTimeAverage, 
			long pStartTimeSkpikeDetection, float pLimit) {
		this.startTimeAverage = pStartTimeAverage;
		this.startTimeSpikeDetection = pStartTimeSkpikeDetection;
		this.limit = pLimit;
	}
	
	public long getStartTimeAverage() {return this.startTimeAverage;}
	public long getStartTimeSpikes() {return this.startTimeSpikeDetection;}
	public float getLimit() {return this.limit;}
	
}
