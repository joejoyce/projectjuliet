package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.util.HashMap;
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
	
	public void pushSingle(Object vl) {
		if(comRq) sb.append(",");
		sb.append("\"");
		sb.append(vl);
		sb.append("\"");
		comRq = true;
	}
	public void mkPair(Object key, Object vl) {
		stOb();
		pushPair(key,vl);
		finOb();
	}
	public void mkMap(String key, Map<?,?> mp) {
		stOb();
		pushMap(key,mp);
		finOb();
	}
	public void pushMap(String key, Map<?,?> mp) {
		pushSingle(key);
		sb.append(":");
		comRq = false;
		pushMap(mp);
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
	
	public static void main(String args[]) {
		JsonBuilder jb = new JsonBuilder();
		HashMap<String,String> hm = new HashMap<String,String>();
		
		for(int i=0;i<1000000;i++) {
			hm.put(Integer.toString(i), Integer.toString(i));
		}
		long init = System.currentTimeMillis();
		jb.pushMap(hm);
		/*jb.stArr();
		jb.stOb();
		jb.pushPair("One", "Two");
		jb.finOb();
		jb.stOb();
		jb.pushPair("four","five");
		jb.finOb();
		jb.finArr();*/
		String str = jb.toString();
		System.out.println("duration " + (System.currentTimeMillis() - init));
		//System.out.println(str);
		
	}
}
