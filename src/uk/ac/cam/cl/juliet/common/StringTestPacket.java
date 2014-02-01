package uk.ac.cam.cl.juliet.common;

public class StringTestPacket extends Container {
	private String msg;
	public StringTestPacket(String msg) {
		this.msg = msg;
	}
	public String toString() {
		return msg;
	}
}
