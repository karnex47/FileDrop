package com.karnex.connectivity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
  
import javax.bluetooth.*;
import javax.microedition.io.*;

import com.karnex.commons.Command;
import com.karnex.commons.Config;
import com.karnex.commons.FileItem;
import com.karnex.scheduler.Scheduler;
  
/**
* Class that implements an SPP Server which accepts single line of
* message from an SPP client and sends a single line of response to the client.
*/
public class BluetoothHandler implements Runnable{
	private static Object lock = new Object();
	private StreamConnection connection;
	private boolean isRunning = false;
	private final int buffersize = 1000;
	private InputStream inStream = null;
	private OutputStream outStream = null;
	
 public BluetoothHandler(){
		//display local device address and name
        LocalDevice localDevice;
		try {
			localDevice = LocalDevice.getLocalDevice();
			System.out.println("Address: "+localDevice.getBluetoothAddress());
	        System.out.println("Name: "+localDevice.getFriendlyName());
		} catch (BluetoothStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		
	}
    
    //start server
    public void startServer() throws IOException{
  
        //Create a UUID for SPP
        UUID uuid = new UUID(Config.uuid, true);
        System.out.println(uuid.toString());
        //Create the servicve url
        String connectionString = "btspp://localhost:" + uuid +";name=Bluetooth SPP Server";
        
        //open server url
        StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier)Connector.open( connectionString );
        
        //Wait for client connection
        System.out.println("\nServer Started. Waiting for clients to connect...");
        while(isRunning){
	        connection=streamConnNotifier.acceptAndOpen();
	        System.out.println("Connection opened");
	        RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
	        System.out.println("Remote device address: "+dev.getBluetoothAddress());
	        System.out.println("Remote device name: "+dev.getFriendlyName(true));
	        
	        inStream = connection.openInputStream();
	        outStream = connection.openOutputStream();
	        
	        if((dev.getFriendlyName(true) == Config.pairedDevice) || true) {
	        	new Receiver().start();
	        	new Sender().start();
	        }
	        
        }
        streamConnNotifier.close();
  
    }

	@Override
	public void run() {
		isRunning = true;
		try {
			startServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void sendString(String msg) throws IOException {
		//send response to spp client
		if(connection == null) return;
		PrintWriter pWriter=new PrintWriter(new OutputStreamWriter(outStream));
        pWriter.write(msg+"\r\n");
        pWriter.flush();
  System.out.println("Out: "+msg);
	}
	
	private void sendFile() {
		//Need to implement
	}
	
	private class Receiver extends Thread {
		public void run() {
			//read string from spp client
			try {
				BufferedReader bReader=new BufferedReader(new InputStreamReader(inStream));
		        String lineRead;
		        while(((lineRead = bReader.readLine()) != null) && connection != null) {
		        	System.out.println(lineRead);
		        	synchronized(lock){
		        		FileItem input = parser(lineRead);
		        		if(input.getCmd() == Command.cp) {
		        			//Open byte stream
		        		}
		        		if(input.getCmd() == Command.ack) {
		        			Scheduler.pollFromQueue();
		        		}
		        	}
		        }
			} catch (IOException e) {
				System.out.println("Receiver closed");
			}
		}
	}
	
	private class Sender extends Thread {
		public void run() {
			while (connection != null) {
				synchronized (lock) {
					FileItem fileItem = Scheduler.peekQueue();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
					FileInputStream fis = null;
					BufferedInputStream bis = null;
					if(fileItem != null) {
						try {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							sendString(fileItem.toString());
							if(fileItem.getCmd() == Command.cp) {
								File file = new File(fileItem.getName());
								long length = file.length();
							    if (length > Integer.MAX_VALUE) {
							        System.out.println("File is too large.");
							    }
							    byte[] bytes = new byte[buffersize];
							    fis = new FileInputStream(file);
							    bis = new BufferedInputStream(fis);
							    System.out.println("Length: "+length);
							    int count;
							    int counter = 0;
							    while(length > 0 && ((count = bis.read(bytes)) > 0)) {
							    	try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
							    	outStream.write(bytes, 0, count);
							    	outStream.flush();
							    	length -= count;
							    	counter++;
							    	System.out.println("Length: "+length);
							    }
							    
							    System.out.println("Outside file sending loop");
							    System.out.println("Counter: "+counter);
							    outStream.flush();
							    fis.close();
							    bis.close();
							}
							outStream.flush();
							Scheduler.pollFromQueue();
						} catch (IOException e) {
							e.printStackTrace();
							try {
								fis.close();
								bis.close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	private FileItem parser(String input) {
		String[] tokens = input.split("|");
		FileItem ret = new FileItem();
		ret.setCmd(Command.valueOf(tokens[0]));
		if(tokens[1] != null)
			ret.setName(tokens[1]);
		return ret;
	}
}
