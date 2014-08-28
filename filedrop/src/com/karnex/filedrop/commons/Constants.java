package com.karnex.filedrop.commons;

public class Constants {
	private static String appDirectory; // To be replaced with shared pref later
	public static String btAddress;
	public static String SPP_UUID;
	public static final int REQUEST_CODE_PICK_DIR = 1;
	public static final int REQUEST_CODE_PICK_FILE = 2;

	public static String getAppDirectory() {
		return appDirectory;
	}

	public static synchronized void setAppDirectory(String appDirectory) {
		Constants.appDirectory = appDirectory;
	}
}
