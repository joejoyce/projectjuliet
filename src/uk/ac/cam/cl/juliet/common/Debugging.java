package uk.ac.cam.cl.juliet.common;


private class outputWrap {
	PrintStream printout = null;
	ObjectOutputStream ojout = null;
	public OutputWrap(PrintStream ps) {
		printout = ps;
	}
	public OutputWrap( ObjectOutputStream oj ) {
		ojout = oj;
	}
	public void send ( String str) {
		if(printout)
			printout.print(str);
		else
			ojout.write( new DebugMsg(str)); //Send message over the network to the ClusterMaster.
	}
}

public class Deubugging {
	private static String myAddr = null;
	private static boolean debug = false;
	
	private static LinkedList<outputWrap> out = new LinkedList<outputWrap>();
	
	public static void setAddr( InetAddr addr ) {
		myAddr = addr.toString();
	}
	public static void setAddr( String addr ) {
		myAddr = addr;
	}
	
	public static void enableDebugging() {
		debug = true;
	}
	
	public static void disableDebugging() {
		debug = false;
	}

	public static void registerOutputLocation( PrintStream ps ) {
		out.add(ps);
	}
	
	public static boolean removeOutputLocation( PrintStream ps) {
		return out.remove(ps);
	}
	
	public static void registerOutputLocation( ObjectOutputStream out ) {
		out.add(out);
	}
	
	public static boolean removeOutputLocation( ObjectOutputStream out ) {
		return out.remove(out);
	}
	
	private static void send(String str) {
		for(OutputWrap o : out) {
			o.send(str);
		}
	}
	
	public static void println(String str) {
		if(debug)
			send(str + "\n");
	}
	
	public static void print(String str) {
		if(debug)
			send(str);
	}
	
	public static void recieveDebug( DebugMsg msg ) {
		send(msg); //TODO also add information about location? 
	}
}
