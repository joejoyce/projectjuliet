package uk.ac.cam.cl.juliet.master;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import uk.ac.cam.cl.juliet.common.ConfigurationPacket;
import uk.ac.cam.cl.juliet.common.Debug;

public class ShutdownSettingsSaver extends Thread {
	private String filename;
	private ConfigurationPacket cp = null;
	public ShutdownSettingsSaver (String filename) {
		this.filename = filename;
	}
	public void run (){
        try {
        	Debug.println(Debug.INFO,"Saving settings on exit");
			File f = new File(filename);
	        FileWriter fw = new FileWriter(f);
	        //write config. pack settings:
	        for (Entry<String, String> entry : cp.getSettings().entrySet()) {
	            String key = entry.getKey();
	            String value = entry.getValue();
	            fw.write("client ");
				fw.write(key);
	            fw.write(" ");
	            fw.write(value);
	            fw.write("\n");
	        }
	        fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Debug.println(Debug.SHOWSTOP,"Error saving settings on exit");
			e.printStackTrace();
		}
	}
	public void setConfigurationPacket(ConfigurationPacket cp) {
		this.cp = cp;
	}
}
