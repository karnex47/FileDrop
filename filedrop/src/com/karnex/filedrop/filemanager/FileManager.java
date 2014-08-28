package com.karnex.filedrop.filemanager;

import com.karnex.filedrop.commons.Constants;

import android.app.Activity;
import android.content.Intent;

public class FileManager {
	
	public static final int REQUEST_CODE_PICK_DIR = 1;
	public static final int REQUEST_CODE_PICK_FILE = 2;
	
	public static void startFolderView(Activity activity, String path) {
		if(path == null) path = "/";
		Intent fileExploreIntent = new Intent(
				FileBrowserActivity.INTENT_ACTION_SELECT_DIR,
				null,
				activity,
				FileBrowserActivity.class
				);
		fileExploreIntent.putExtra(
				FileBrowserActivity.startDirectoryParameter, 
				path
				);
		activity.startActivityForResult(fileExploreIntent, FileManager.REQUEST_CODE_PICK_DIR);
	}
	
	public static void startFileView(Activity activity, String path) {
		if(path == null) path = "/";
		Intent fileExploreIntent = new Intent(
				FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
				null,
				activity,
				FileBrowserActivity.class
				);
		fileExploreIntent.putExtra(
				FileBrowserActivity.startDirectoryParameter, 
				path
				);
		activity.startActivity(fileExploreIntent);
	}
	
	public static void startMainWindowActivity(Activity activity) {
		String path = Constants.getAppDirectory();
		Intent fileExploreIntent = new Intent(
				FileBrowserActivity.INTENT_ACTION_SELECT_MAIN,
				null,
				activity,
				FileBrowserActivity.class
				);
		fileExploreIntent.putExtra(
				FileBrowserActivity.startDirectoryParameter, 
				path
				);
		activity.startActivity(fileExploreIntent);
	}
}
