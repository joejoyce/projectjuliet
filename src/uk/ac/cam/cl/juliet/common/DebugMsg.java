package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;

public class DebugMsg extends Container implements Serializable {
	private String msg;
	private int priority;
	public DebugMsg(String msg) {
		this.msg = msg;
		this.priority = 0;
	}
	public DebugMsg(String msg, int pri) {
		this.msg = msg;
		this.priority = pri;
	}
	public String toString() {
		return msg;
	}
	public int getPriority() {
		return priority;
	}
}
