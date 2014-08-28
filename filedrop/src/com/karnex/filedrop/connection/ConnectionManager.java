package com.karnex.filedrop.connection;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;


public class ConnectionManager {

	private static final String TAG = "BluetoothMesseging";
	private static final boolean D = false;

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	// Store the current connected Device Address;
		private String mCurrentDeviceAddress = null;
		// Name of the connected device
		private String mConnectedDeviceName = null;
		private ConnectThread mConnectThread;
		
		// Local Bluetooth adapter
		private BluetoothAdapter mBluetoothAdapter = null;
		// Member object for the chat services
		private BluetoothMeterService mChatService = null;
		
		// Automatically try to connec with the known mac address;
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
}
