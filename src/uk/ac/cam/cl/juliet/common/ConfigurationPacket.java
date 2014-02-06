package uk.ac.cam.cl.juliet.common;

import java.io.Serializable;
import java.util.HashMap;

public class ConfigurationPacket extends Container implements Serializable {
	HashMap <String,String>h = new HashMap<String,String>();
	public String getSetting(String name) {
		return h.get(name);
	}
	public void setSetting(String name, Object ob) {
		h.put(name,ob.toString());
	}
}
