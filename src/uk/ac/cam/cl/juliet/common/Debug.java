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

public class Debug {
	private static String myAddr = null;
	
	public static int SHOWSTOP = 40, ERROR = 30, WARN = 20;
	public static int INFO = 10, DEBUG = 0, ALL = -10; 
	
	
	private static int default_priority = 0;
	private static int priority = default_priority;
	
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
		if(priority <= default_priority)
			send(str + "\n",default_priority);
	}
	
	public static void print(String str) {
		if(priority <= default_priority)
			send(str,default_priority);
	}
	public static void println(int pri, String str) {
		if(priority <= pri)
			send(str + "\n",pri);
	}
	
	public static void print(int pri, String str) {
		if(priority <= pri)
			send(str,pri);
	}
	public static void printStackTrace(int pri, String str) {
		if(priority <= pri)
			send(str,pri);
	}
	public static void printStackTrace(Exception e) {
		if(priority <= Debug.ERROR)
			send(e.getStackTrace().toString(),Debug.ERROR);
	}
	
	public static void recieveDebug( DebugMsg msg ) {
		if(priority >= msg.getPriority())
			send(msg.toString(),msg.getPriority()); //TODO also add information about location? 
	}
	
	public static boolean parseDebugArgs(String input) {
		if(input.matches("debug:\\s*\\w*")) {
			String dargs[] = input.split(":");
			if(dargs.length > 1) {
				switch(dargs[1].trim()) {
					case "ALL":
						Debug.setPriority(Debug.ALL);
						break;
					case "DEBUG":
						Debug.setPriority(Debug.DEBUG);
						break;
					case "INFO":
						Debug.setPriority(Debug.INFO);
						break;
					case "WARN":
						Debug.setPriority(Debug.WARN);
						break;
					case "ERROR":
						Debug.setPriority(Debug.ERROR);
						break;
					case "SHOWSTOP":
						Debug.setPriority(Debug.SHOWSTOP);
						break;
					default :
						System.err.println(dargs[1] + " is not a valid debug level");
				}
			} else {
				System.err.println("Malformed debug level expression");
			}
		} else {
			return false;
		}
		return true;
	}
}
