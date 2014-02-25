package uk.ac.cam.cl.juliet.master.clustermanagement.queryhandling;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NotificationsList {
	
	
	private ConcurrentLinkedQueue<Notification> ll = new ConcurrentLinkedQueue<Notification>();
	
	public void addNotification(String title, String body) {
		Notification n = new Notification(title,body);
		ll.add(n);
	}
	public void addNotification(String title, String body, long stockId) {
		Notification n = new Notification(title,body,stockId);
		ll.add(n);
	}
	private void clean() {
		Iterator<Notification> i = ll.iterator();
		long time = System.nanoTime();
		while(i.hasNext()) {
			Notification n = i.next();
			if(time >= n.timeout)
				i.remove();
			else
				return;
		}
	}
	public Collection<Notification>getNotifications() {
		return getNotifications(Long.MIN_VALUE);
	}
	/**
	 * Returns a list of notifications that have happened since since.
	 * to work out
	 * @param since
	 * @return
	 */
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
		long fetchTime = 0;
		Collection<Notification> c = getNotifications(since);
		
		Iterator<Notification>i = c.iterator();
		JsonBuilder jb = new JsonBuilder();
		jb.stArr("Messages");
		Notification n = null;
		while(i.hasNext()) {
			n = i.next();
			jb.stOb();
			jb.pushPair("title", n.title);
			jb.pushPair("body", n.body);
			jb.finOb();
			
		}
		jb.finArr();
		
		if(null != n)
			fetchTime = n.timestamp;
		else
			fetchTime = since;
		jb.pushPair("updateStamp", fetchTime);
		
		return jb.toString();
	}
	public String getNotificationsJson() {
		return getNotificationsJson(Long.MIN_VALUE);
	}
	
	public void pushNotification(String title, String body) {
		clean();
		ll.add(new Notification(title,body));
	}
	
	
/*	public static void main(String args[]) {
		NotificationsList note = new NotificationsList();
		note.addNotification("O RLY","NO WAI");
		System.out.println(note.getNotificationsJson());
		try {
			Thread.sleep(11000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long nanoTime = System.nanoTime();
		note.addNotification("Hello","world");
		System.out.println(note.getNotificationsJson());
		System.out.println(note.getNotificationsJson(nanoTime));
	} */
}
