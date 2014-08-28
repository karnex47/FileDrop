package com.karnex.commons;

public class Config {
	private static String homePath;
	public static String uuid = "1101";
	public static String pairedDevice;

	public static String getHomePath() {
		return homePath;
	}

	public static void setHomePath(String homePath) {
		Config.homePath = homePath;
	}
	
}
