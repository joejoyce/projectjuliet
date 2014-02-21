package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

class Notification {
	public String title;
	public String body;
	public long timestamp;
	public long timeout;
	private static long defKeepLength = 5000000000L; //5 seconds
	
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
}

public class NotificationsList {
	
	
	private ConcurrentLinkedQueue<Notification> ll = new ConcurrentLinkedQueue<Notification>();
	
	public void addNotification(String title, String body) {
		
	}
	private void clean() {
		Iterator<Notification> i = ll.iterator();
		long time = System.nanoTime();
		while(i.hasNext()) {
			Notification n = i.next();
			if(time >= n.timeout)
				i.remove();
		}
	}
	public Collection<Notification>getNotifications() {
		return getNotifications(Long.MIN_VALUE);
	}
	public Collection<Notification>getNotifications(long since) {
		clean();
		LinkedList<Notification>l = new LinkedList<Notification>();
		Iterator<Notification>i = ll.iterator();
		while(i.hasNext()) {
			Notification n = i.next();
			if(n.timestamp > since)
				l.add(n);
		}
		return l;
	}
	public String getNotificationsJson(long since) {
		Collection<Notification> c = getNotifications(since);
		Iterator<Notification>i = c.iterator();
		JsonBuilder jb = new JsonBuilder();
		jb.stArr();
		while(i.hasNext()) {
			Notification n = i.next();
			jb.stOb();
			jb.pushPair("title", n.title);
			jb.pushPair("body", n.body);
			jb.finOb();
		}
		jb.finArr();
		return jb.toString();
	}
	public String getNotificationsJson() {
		return getNotificationsJson(Long.MIN_VALUE);
	}
	
	
	public void pushNotification(String title, String body) {
		clean();
		ll.add(new Notification(title,body));
	}
}
