package uk.ac.cam.cl.juliet.common;

import java.util.HashMap;

public class ConfigurationPacket extends Container {
	HashMap <String,String>h = new HashMap<String,String>();
	public String getSetting(String name) {
		return h.get(name);
	}
	public void setSetting(String name, Object ob) {
		h.put(name,ob.toString());
	}
}
