package com.karnex.filedrop.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

import com.karnex.filedrop.commons.Command;
import com.karnex.filedrop.commons.Config;
import com.karnex.filedrop.commons.FileItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
public class BluetoothMeterService {
	// Debugging
	private static final String TAG = "BluetoothMeterService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME = "BluetoothMeter";

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	// we're doing nothing;
	public static final int STATE_NONE = 0;
	// now listening for incoming connections;
	// public static final int STATE_LISTEN = 1;
	// now initiating an outgoing connection
	public static final int STATE_CONNECTING = 2;
	// now connected to a remote device
	public static final int STATE_CONNECTED = 3;

	/**
	 * Constructor. Prepares a new BluetoothMeter session.
	 * @param console 
	 * 
	 * @param context
	 *            The UI Activity Context
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothMeterService(Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(Console.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			// mConnectThread = null;
		}
		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			// mConnectedThread = null;
		}

		setState(STATE_NONE);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		if (D)
			Log.d(TAG, "Start to connect.");
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(Console.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(Console.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		// msg.setTarget(mHandler);
		// msg.sendToTarget();
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop apublic static final int MESSAGE_STATE_CHANGE = 1; public static
	 * final int MESSAGE_READ = 2; public static final int MESSAGE_WRITE = 3;
	 * public static final int MESSAGE_DEVICE_NAME = 4; Consolereads
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param string
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(String string) {
		if (string == null)
			return;
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		try {
			r.sendFile(string);
		} catch (IOException e) {
			Log.d(TAG, "error on calling send file");
			e.printStackTrace();
		}
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_NONE);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(Console.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(Console.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_NONE);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(Console.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(Console.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
//	private class AcceptThread extends Thread {
//		// The local server socket
//		private final BluetoothServerSocket mmServerSocket;
//
//		public AcceptThread() {
//			BluetoothServerSocket tmp = null;
//
//			// Create a new listening server socket
//			try {
//				tmp = mAdapter
//						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
//			} catch (IOException e) {
//				Log.e(TAG, "listen() failed", e);
//			}
//			mmServerSocket = tmp;
//		}
//
//		public void run() {
//			if (D)
//				Log.d(TAG, "BEGIN mAcceptThread" + this);
//			setName("AcceptThread");
//			BluetoothSocket socket = null;
//
//			// Listen to the server socket if we're not connected
//			while (mState != STATE_CONNECTED) {
//				try {
//					// This is a blocking call and will only return on a
//					// successful connection or an exception
//					socket = mmServerSocket.accept();
//				} catch (IOException e) {
//					Log.e(TAG, "accept() failed", e);
//					break;
//				}
//
//				// If a connection was accepted
//				if (socket != null) {
//					synchronized (BluetoothMeterService.this) {
//						switch (mState) {
//						case STATE_CONNECTING:
//							// Situation normal. Start the connected thread.
//							connected(socket, socket.getRemoteDevice());
//							break;
//						case STATE_NONE:
//						case STATE_CONNECTED:
//							// Either not ready or already connected. Terminate
//							// new socket.
//							try {
//								socket.close();
//							} catch (IOException e) {
//								Log.e(TAG, "Could not close unwanted socket", e);
//							}
//							break;
//						}
//					}
//				}
//			}
//			if (D)
//				Log.i(TAG, "END mAcceptThread");
//		}
//
//		public void cancel() {
//			if (D)
//				Log.d(TAG, "cancel " + this);
//			try {
//				mmServerSocket.close();
//			} catch (IOException e) {
//				Log.e(TAG, "close() of server failed", e);
//			}
//		}
//	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			if (D)
				Log.d(TAG, "Create connection.");
			try {
				// final UUID SPP_UUID = UUID
				// .fromString("00001101-0000-1000-8000-00805F9B34FB");
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				if (D)
					Log.d(TAG, tmp.getRemoteDevice().getAddress());
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			if (D)
				Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
				if (D)
					Log.d(TAG, "Trying to make connected");
			} catch (IOException e) {
				if (D) {
					Log.d(TAG, "Connect failed;");
					Log.e(TAG, e.getMessage());
				}
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				// Start the service over to restart listening mode
				BluetoothMeterService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothMeterService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			if (D)
				Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			if (D)
				Log.d(TAG, "BEGIN mConnectedThread");
//			int bufferSize = 4096;
//			byte[] buffer = new byte[bufferSize];

			// Keep listening to the InputStream while connected
//			while (true) {
//				try {
//					// Read from the InputStream
//					int bytesRead = -1;
//					String message = "";
//					if (mmInStream.available() > 0) {
//						bytesRead = mmInStream.read(buffer);
//						if (bytesRead > 0) {
//							while ((bytesRead == bufferSize)
//									&& (buffer[bufferSize - 1] != 0)) {
//								message = message
//										+ new String(buffer, 0, bytesRead);
//								bytesRead = mmInStream.read(buffer);
//							}
//							 need to implement the handling of recieved files may be open new file by same filename passed and write in it
//							if ((buffer[bytesRead - 1] != 0)) {
//								message = message
//										+ new String(buffer, 0, bytesRead);
//							} else {
//								message = message
//										+ new String(buffer, 0, bytesRead - 1);
//							}
//							
//						}
//					}
//				} catch (IOException e) {
//					Log.e(TAG, "disconnected", e);
//					connectionLost();
//					break;
//				}
//			}
			final int buffersize = 1000;
	        BufferedReader bReader=new BufferedReader(new InputStreamReader(mmInStream));
	        String lineRead;
	        try {
				while((lineRead = bReader.readLine()) != null) {
					Log.d("Input", lineRead);
					FileOutputStream fos = null;
					BufferedOutputStream bos = null;
					FileItem fileItem = FileItem.parse(lineRead.trim());
					if(fileItem != null) {
						Log.d("IF_cond_check", Boolean.valueOf(fileItem.getCmd().equals("cp")).toString());
						if(fileItem.getCmd().equals("cp")) {
							Log.d("Length",""+fileItem.getFileSize());
							fos = new FileOutputStream(fileItem.getName());
						    bos = new BufferedOutputStream(fos);
						    long fileSize = fileItem.getFileSize();
						    byte[] bytes = new byte[buffersize];
						    int counter = 0;
						    int count;
						    while (fileSize > 0 && (count = mmInStream.read(bytes)) > 0) {
						        bos.write(bytes, 0, count);
						        bos.flush();
						        fileSize -= count;
						        counter++;
						        Log.d("Counter", ""+counter);
						        Log.d("Count", ""+count);
						        Log.d("Length", ""+fileSize);
						    }
						    bos.flush();
						    bos.close();
						    fos.close();
						 
							Log.d("Checking CP","CP");
						}else if(fileItem.getCmd().equals("rm")){
							
							File del = new File(fileItem.getName());
							if(del!=null && del.exists()){
								del.delete();
								Log.d("File deleted", fileItem.getName());
							}
						}else if(fileItem.getCmd().equals("mkdir")){
							File del = new File(fileItem.getName());
							if(!del.exists()) {
								del.mkdir();
								Log.d("File created", fileItem.getName());
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void sendFile(String path) throws IOException {
			Log.d(TAG, "sending data");
			File rootsd = Environment.getExternalStorageDirectory();
			File file = new File(rootsd.getAbsolutePath() + path);
			byte[] buffer = new byte[(int) file.length()];
			BufferedInputStream bReader;
			FileInputStream fis = null;	
			
			Log.d(TAG, "obtained input stream here in recentDevices Activity");
			try {

				fis = new FileInputStream(file);
				bReader = new BufferedInputStream(fis);

				// we need to know how may bytes were read to write them to the
				// byteBuffer
				bReader.read(buffer, 0, buffer.length);
                mmOutStream.write(buffer, 0, buffer.length);
                mmOutStream.flush();
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
				// Close streams;
				mmInStream.close();
				mmOutStream.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
