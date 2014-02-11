package uk.ac.cam.cl.juliet.common;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;


class OutputWrap {
	PrintStream printout = null;
	ObjectOutputStream ojout = null;
	Queue<Container> q = null;
	public OutputWrap(PrintStream ps) {
		printout = ps;
	}
	public OutputWrap( ObjectOutputStream oj ) {
		ojout = oj;
	}
	public OutputWrap(Queue<Container>q ){
		this.q = q;
	}
	public void send ( String str, int pri) {
		if(printout != null)
			printout.print(str); //Hopefully this doesn't need to be thread safe
		else if (null != q) {
			q.add( new DebugMsg(str));
		} else {
			try {
				ojout.writeObject( new DebugMsg(str,pri));
			} catch (IOException e) {
				System.err.println("Error sending error to host!");
			}
		}
	}
}

public class Debugging {
	private static String myAddr = null;
	private static int priority = 0;
	private static int default_priority = 5;
	
	private static LinkedList<OutputWrap> out = new LinkedList<OutputWrap>();
	
	public static void setAddr( InetAddress addr ) {
		myAddr = addr.toString();
	}
	public static void setAddr( String addr ) {
		myAddr = addr;
	}
	
	public static void setPriority(int pri) {
		priority = pri;
	}
	public static int getPriority() {
		return priority;
	}

	public static void registerOutputLocation( PrintStream ps ) {
		out.add(new OutputWrap(ps));
	}
	
	public static void registerOutputLocation( ObjectOutputStream oj ) {
		out.add(new OutputWrap(oj));
	}
	
	
	/**
	 * Probably use this on the client as ObjectOutputStream is not thread safe
	 * @param q The queue to put return messages on
	 */
	public static void registerOutputLocation( Queue<Container> q) {
		out.add(new OutputWrap(q));
	}
	
	private static void send(String str, int pri) {
		for(OutputWrap o : out) {
			o.send(str,pri);
		}
	}
	
	public static void println(String str) {
		if(priority >= default_priority)
			send(str + "\n",default_priority);
	}
	
	public static void print(String str) {
		if(priority >= default_priority)
			send(str,default_priority);
	}
	public static void println(int pri, String str) {
		if(priority >= pri)
			send(str + "\n",pri);
	}
	
	public static void print(int pri, String str) {
		if(priority >= pri)
			send(str,pri);
	}
	
	public static void recieveDebug( DebugMsg msg ) {
		if(priority >= msg.getPriority())
			send(msg.toString(),msg.getPriority()); //TODO also add information about location? 
	}
}
