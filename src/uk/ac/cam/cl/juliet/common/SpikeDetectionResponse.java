package uk.ac.cam.cl.juliet.common;
/**
 * This is the response to a SpikeDetectionRequest. It has the stock symbols for 
 * which a spike got detected and the time in seconds.
 * Both information is stored in arrays with a low initial size. Should the number of 
 * spikes exceed the size, the arrays are enlarged by the initial array size.
 * This implementation is chosen to keep this object small in size because they are
 * send over the network. Furthermore a spike is considered rare and we don't expect
 * to see a large number of spikes at one check.
 * @author lucas
 *
 */
public class SpikeDetectionResponse extends QueryResponse {
	
	private static final long serialVersionUID = 1L;
	private static final int initialArraySize = 5;
	private String[] symbolsOfstockWithSpike;
	private long[] timesOfSpike;
	private int numberOfDetectedSpikes;

	public SpikeDetectionResponse(long id, boolean result) {
		super(id, result);
		this.symbolsOfstockWithSpike = new String[initialArraySize];
		this.timesOfSpike = new long[initialArraySize];
	}
	
	public void addSpike(String symbol, long time) {
		//in case the array to store the spikes is not large enough
		if(this.symbolsOfstockWithSpike.length >= numberOfDetectedSpikes) {
			//enlarge array of stock names
			String[] temps = this.symbolsOfstockWithSpike;
			this.symbolsOfstockWithSpike = new String[numberOfDetectedSpikes+initialArraySize];
			for(int i = 0; i < numberOfDetectedSpikes;i++)
				this.symbolsOfstockWithSpike[i] = temps[i];
			//enlarge array of times
			long[] templ = this.timesOfSpike;
			this.timesOfSpike = new long[numberOfDetectedSpikes+initialArraySize];
			for(int i = 0; i < numberOfDetectedSpikes;i++)
				this.timesOfSpike[i] = templ[i];
		}
		this.numberOfDetectedSpikes++;
			
		this.symbolsOfstockWithSpike[numberOfDetectedSpikes] = symbol;
		this.timesOfSpike[numberOfDetectedSpikes] = time;
	}
	

}
