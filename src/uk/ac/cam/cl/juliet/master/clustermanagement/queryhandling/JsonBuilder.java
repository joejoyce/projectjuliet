package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

public class JsonBuilder {
	private StringBuilder sb = new StringBuilder();
	private boolean comRq = false;
	
	public void stArr() {
		if(comRq) sb.append(",");
		sb.append("[");
		comRq = false;
	}
	public void finArr() {
		sb.append("]");
		comRq = true;
	}
	
	public void stOb() {
		if(comRq)sb.append(",");
		sb.append("{");
		comRq = false;
	}
	public void finOb() {
		sb.append("}");
		comRq = true;
	}
	public void pushPair(Object key, Object vl) {
		if(comRq) sb.append(",");
		sb.append("\"");
		sb.append(key);
		sb.append("\" : \"");
		sb.append(vl);
		sb.append("\"");
		comRq = true;
	}
	
	public void pushSingle(Object key, Object vl) {
		if(comRq) sb.append(",");
		sb.append("\"");
		sb.append(key);
		sb.append("\"");
		comRq = true;
	}
	public void mkPair(Object key, Object vl) {
		stOb();
		pushPair(key,vl);
		finOb();
	}
	
	public void pushMap(Map<?,?> mp) {
		stArr();
	    Iterator<?> it = mp.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<?,?> pairs = (Entry<?, ?>)it.next();
	        mkPair(pairs.getKey(),pairs.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    finArr();
	}
	
	public String toString() {
		return sb.toString();
	}
}
