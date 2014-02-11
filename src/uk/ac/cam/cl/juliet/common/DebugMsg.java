package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

public class DebugMsg extends Container implements Serializable {
	private String msg;
	public StringTestPacket(String msg) {
		this.msg = msg;
	}
	public String toString() {
		return msg;
	}
}
