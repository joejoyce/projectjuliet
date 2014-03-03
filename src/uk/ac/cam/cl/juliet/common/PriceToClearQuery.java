package uk.ac.cam.cl.juliet.common;

public class PriceToClearQuery extends QueryPacket{
	public int volume;
	public int stockId;
	public PriceToClearQuery() {
		this.setHighPriority();
	}
}
