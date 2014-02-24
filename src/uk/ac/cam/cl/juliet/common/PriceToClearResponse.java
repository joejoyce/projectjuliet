package uk.ac.cam.cl.juliet.common;

public class PriceToClearResponse extends QueryResponse{
	public PriceToClearResponse(long id, boolean result) {
		super(id, result);
		// TODO Auto-generated constructor stub
	}
	public int volume;
	public int stockId;
	public double price;
	public boolean fullyMet = false;
}
