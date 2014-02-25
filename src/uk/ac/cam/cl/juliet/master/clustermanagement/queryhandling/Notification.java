package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

public class Notification {
	public String title;
	public String body;
	public long timestamp;
	public long timeout;
	public long relatedStockId;
	private static long defKeepLength = 10000000000L; //10 seconds
	
	public Notification(String title, String body){
		this.title = title;
		this.body = body;
		this.timestamp = System.nanoTime();
		this.timeout = timestamp + defKeepLength;
	}
	public Notification(String title, String body, long timeout){
		this.title = title;
		this.body = body;
		this.timestamp = System.nanoTime();
		this.timeout = timestamp + timeout;
	}
	public Notification(String title, String body, long timeout,long relatedStockId){
		this.title = title;
		this.body = body;
		this.timestamp = System.nanoTime();
		this.timeout = timestamp + timeout;
		this.relatedStockId = relatedStockId;
	}
}

