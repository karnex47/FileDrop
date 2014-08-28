package com.karnex.filedrop.commons;

import android.content.Context;

import com.karnex.filedrop.connection.BluetoothBackgroundTask;
import com.karnex.filedrop.connection.Console;

public class Config {
	private static String homePath;
	public static String uuid = "1101";
	public static Console currConnection;
	public static BluetoothBackgroundTask bluetoothBackgroundTask;
	public static Context context;

	public static String getHomePath() {
		return homePath;
	}

	public static void setHomePath(String homePath) {
		Config.homePath = homePath;
	}
	
}
