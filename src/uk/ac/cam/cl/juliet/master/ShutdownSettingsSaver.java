package uk.ac.cam.cl.juliet.master;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Debug;
import uk.ac.cam.cl.juliet.master.dataprocessor.DataProcessor;
import uk.ac.cam.cl.juliet.master.dataprocessor.XDPDataStream;

public class ShutdownSettingsSaver {
	private String filename;
	private ConfigurationPacket cp = null;
	private DataProcessor dp;
	
	public ShutdownSettingsSaver (String filename) {
		this.filename = filename;
		Debug.println(Debug.INFO, "created a new ShutdownSettingsSaver");
	}
	
	//I don't think that any of the debug-print-statements are executed as the system is closing -lucas
	public void savePointers() {
        try {
        	XDPDataStream xdpStream = dp.getDataStream();
        	Debug.println(Debug.INFO,"Saving settings on exit");
			File f = new File(filename);
	        FileWriter fw = new FileWriter(f);
	        
        	dp.pause = true;
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	
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
	        if(xdpStream == null) {
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
	        fw.flush();
	        fw.close();
		} catch (IOException e) {
			Debug.println(Debug.SHOWSTOP,"Error saving settings on exit");
			e.printStackTrace();
		}
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void setConfigurationPacket(ConfigurationPacket cp) {
		this.cp = cp;
	}
	
	public void setDataStream(DataProcessor ds) {
		this.dp = ds;
	}
}
