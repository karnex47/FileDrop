/**
 * 
 */
package com.karnex.filedrop.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import com.karnex.filedrop.R;
import com.karnex.filedrop.commons.Config;
import com.karnex.filedrop.commons.FileItem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Antonio081014
 * @time: May 14, 2013, 2:44:35 PM
 */
public class Console{

	// Debugging
	private static final String TAG = "BluetoothMesseging";
	private static final boolean D = true;

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	private static Queue<com.karnex.filedrop.commons.FileItem> queue = new LinkedList<FileItem>();
	// Store the current connected Device Address;
	private String mCurrentDeviceAddress = null;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	private BluetoothSocket btSocket = null;
	private ConnectThread mConnectThread;
	private SharedPreferences settings;
	// private String bufferMessege;
	
	private Context mContext;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothMeterService mChatService = null;

	public Console(Context context){
		mContext = context;
		settings = mContext.getSharedPreferences(BluetoothDeviceList.PREFS_NAME, 0);
		mChatService = new BluetoothMeterService(mHandler);
		mCurrentDeviceAddress = settings.getString(
				BluetoothDeviceList.PREFS_DEVICE_ADDR, null);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		connectDevice();
	}
	
	public void execute() {
		connectDevice();
	}
	
	// The Handler that gets information back from the BluetoothMeterService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothMeterService.STATE_CONNECTED:
					// check the Q and start sending
					Log.d("CONNECT_CHECK", "Its connected");
					if(!queue.isEmpty()){
						FileItem fileToTransfer = queue.peek();
						initTransfer(fileToTransfer.getName());
					}
					// if transfer complete on ack remove the item from queue
					break;
				case BluetoothMeterService.STATE_CONNECTING:
					Log.d("CONNECTING", "Its still connecting");
					break;
				case BluetoothMeterService.STATE_NONE:
					Log.d("NO STATE", "No state");
					break;
				}
				break;
			default:
				break;
//			case MESSAGE_WRITE:
//				break;
//			case MESSAGE_READ:
//				byte[] readBuf = (byte[]) msg.obj;
//				// construct a string from the valid bytes in the buffer
//				String readMessage = new String(readBuf, 0, msg.arg1);
//				// bufferMessege += readMessage;
//				
//				// bufferMessege = "";
//				break;
//			case MESSAGE_DEVICE_NAME:
//				// save the connected device's name
//				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//				if (D)
//					Log.d("MESSAGE DEVICE NAME", "Connected to " + mConnectedDeviceName);
//				break;
//			case MESSAGE_TOAST:
//				Log.d("TOAST", "Toast");
//				break;
			}
		}
	};

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void initTransfer(String path) {
		// Check that we're actually connected before trying anything
		if (mChatService != null
				&& mChatService.getState() != BluetoothMeterService.STATE_CONNECTED) {
			return;
		}
		// check the queue if there is anything send it
		// Check that there's actually something to send
		if (mChatService != null && path.length() > 0) {
			
			mChatService.write(path);
		}
	}

	// Automatically try to connect with the known mac address;
	private class ConnectThread extends Thread {

		private final BluetoothDevice device;

		public ConnectThread(BluetoothDevice d) {
			this.device = d;
		}

		public void run() {
			while (mConnectThread == Thread.currentThread()) {
				if (mChatService.getState() == BluetoothMeterService.STATE_CONNECTED) {
					if (D)
						Log.d(TAG, "Device Connected");
						
					// if (dialogref.isShowing())
					// dismissDialog(Dialog_Connect);
					// Ready to use;
					break;
				} else if (mChatService.getState() == BluetoothMeterService.STATE_CONNECTING) {
					try {
						if (D)
							Log.d(TAG, "Connecting...");
						Thread.sleep(2000);
					} catch (Exception e) {
						// Log.e(TAG, e.getMessage());
					}
				} else
					try {
						if (D)
							Log.d(TAG, "Started to Connect");
						
						mChatService.connect(device);
						Thread.sleep(3000);
					} catch (Exception e) {
						// Log.e(TAG, e.getMessage());
						Thread.currentThread().interrupt();
					}
			}
		}
	}

	// create the bluetooth device object, and try to connect with it
	// consistantly and automatically.
	private void connectDevice() {
		if (mCurrentDeviceAddress == null) {
			Log.d("ERROR","Bluetooth MAC address is not assigned.");
			//finish();
			return;
		}
		BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(mCurrentDeviceAddress);
			mConnectThread = new ConnectThread(device);
			mConnectThread.start();
		
	}
	public void stopThreads(){
		Config.currConnection = null;
		if (mConnectThread != null) {
			if (mConnectThread.isAlive())
				mConnectThread.interrupt();
			mConnectThread = null;
		}
		if (mChatService != null) {
			mChatService.stop();
			mChatService = null;
		}
	}

//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		if (mConnectThread != null) {
//			if (mConnectThread.isAlive())
//				mConnectThread.interrupt();
//			mConnectThread = null;
//		}
//		if (mChatService != null) {
//			mChatService.stop();
//			mChatService = null;
//		}
//	}
//
//	@Override
//	protected void onStart() {
//		super.onStart();
//		settings = getSharedPreferences(BluetoothDeviceList.PREFS_NAME, 0);
//		mChatService = new BluetoothMeterService(this, mHandler);
//		mMessage = new ArrayList<Console.CustomizedMessage>();
//		mCurrentDeviceAddress = settings.getString(
//				BluetoothDeviceList.PREFS_DEVICE_ADDR, null);
//
//		connectDevie();
//	}
//
//	@Override
//	public synchronized void onResume() {
//		super.onResume();
//		if (mChatService != null) {
//			if (mChatService.getState() == BluetoothMeterService.STATE_NONE) {
//				mChatService.start();
//			}
//		}
//	}
//
//	@Override
//	public synchronized void onPause() {
//		super.onPause();
//		if (mChatService != null)
//			mChatService.stop();
//	}
//
//	@Override
//	public synchronized void onStop() {
//		if (D)
//			Log.d(TAG, "-- ON STOP --");
//		super.onStop();
//
//		if (mConnectThread != null) {
//			if (mConnectThread.isAlive())
//				mConnectThread.interrupt();
//			mConnectThread = null;
//		}
//
//		mMessage = null;
//
//		if (mChatService != null) {
//			mChatService.stop();
//			mChatService = null;
//		}
//
//	}
}
