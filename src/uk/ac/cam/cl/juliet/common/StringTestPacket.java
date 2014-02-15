package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

public class StringTestPacket extends Container implements Serializable {
	private static final long serialVersionUID = 1L;
	private String msg;
	public StringTestPacket(String msg) {
		this.msg = msg;
	}
	public String toString() {
		return msg;
	}
}
