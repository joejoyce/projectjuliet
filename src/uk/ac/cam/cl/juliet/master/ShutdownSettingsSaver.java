package uk.ac.cam.cl.juliet.master;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;

public class ShutdownSettingsSaver extends Thread {
	private String filename;
	private ConfigurationPacket cp = null;
	private XDPDataStream xdpStream;
	
	public ShutdownSettingsSaver (String filename) {
		this.filename = filename;
	}
	public void run (){
        try {
        	Debug.println(Debug.INFO,"Saving settings on exit");
			File f = new File(filename);
	        FileWriter fw = new FileWriter(f);
	        //write config. pack settings:
	        if(cp == null) {
	        	Debug.println(Debug.ERROR, "Could not save current settings of clients");
	        } else {
	        	for (Entry<String, String> entry : cp.getSettings().entrySet()) {
	        		String key = entry.getKey();
	        		String value = entry.getValue();
	        		fw.write("client ");
	        		fw.write(key);
	        		fw.write(" ");
	        		fw.write(value);
	        		fw.write("\n");
	        	}
	        }
	        //write current position of the xdpDataStream
	        if(this.xdpStream == null) {
	        	Debug.println(Debug.ERROR, "Could not save current position of datastream");
	        } else {
	        	Map<String, String> xdpStreamSettings = xdpStream.endAndGetSettings();
	        	for (Entry<String, String> entry : xdpStreamSettings.entrySet()) {
		            String key = entry.getKey();
		            String value = entry.getValue();
		            fw.write("datastream ");
					fw.write(key);
		            fw.write(" ");
		            fw.write(value);
		            fw.write("\n");
		        }
	        }
	        
	        fw.close();
		} catch (IOException e) {
			Debug.println(Debug.SHOWSTOP,"Error saving settings on exit");
			e.printStackTrace();
		}
	}
	public void setConfigurationPacket(ConfigurationPacket cp) {
		this.cp = cp;
	}
	
	public void setDataStream(XDPDataStream ds) {
		this.xdpStream = ds;
	}
}
