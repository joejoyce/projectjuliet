package uk.ac.cam.cl.juliet.common;

public class SpikeDetectionResponse extends QueryResponse {
	
	private static final long serialVersionUID = 1L;
	private String[] symbolsOfstockWithSpike;
	private long[] timesOfSpike;
	private int numberOfDetectedSpikes;

	public SpikeDetectionResponse(long id, boolean result) {
		super(id, result);
		this.symbolsOfstockWithSpike = new String[5];
		this.timesOfSpike = new long[5];
	}
	
	public void addSpike(String symbol, long time, int index) {
		this.symbolsOfstockWithSpike[index] = symbol;
		this.timesOfSpike[index] = time;
	}

}
