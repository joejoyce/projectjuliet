package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationPacket extends Container implements Serializable {
	private static final long serialVersionUID = 1L;
	ConcurrentHashMap <String,String>h = new ConcurrentHashMap<String,String>();
	public String getSetting(String name) {
		return h.get(name);
	}
	public void setSetting(String name, Object ob) {
		h.put(name,ob.toString());
	}
}
