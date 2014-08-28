package com.karnex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.karnex.commons.Config;
import com.karnex.connectivity.BluetoothHandler;
import com.karnex.watcher.WatchDir;

public class Main {

	public static void main(String[] args) throws IOException {
		
		
        // parse arguments
        if (args.length > 2)
            usage();
        boolean recursive = true;
        Path dir;
        if (args.length == 0) {
        	dir = Paths.get("C:\\Users\\Diptarka\\FDDir");
        }
        else {
	        int dirArg = 0;
	        if (args[0].equals("-r")) {
	            if (args.length < 2)
	                usage();
	            recursive = true;
	            dirArg++;
	        }
	        
	     // register directory and process its events
	        dir = Paths.get(args[dirArg]);
        }
        Config.setHomePath(dir.toString());
        
        Thread bluetoothThread = new Thread(new BluetoothHandler());
        bluetoothThread.start();
        Thread watcherThread = new Thread(new WatchDir(dir, recursive));
        watcherThread.start();
    }
	
	static void usage() {
        System.err.println("usage: java WatchDir [-r] dir");
        System.exit(-1);
    }
}
