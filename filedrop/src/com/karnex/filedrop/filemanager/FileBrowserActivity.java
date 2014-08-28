package com.karnex.filedrop.filemanager;

//Heavily based on code from
//https://github.com/mburman/Android-File-Explore
//	Version of Aug 13, 2011
//Also contributed:
//  Sugan Krishnan (https://github.com/rgksugan) - Jan 2013.
//

//Project type now is Android library: 
//  http://developer.android.com/guide/developing/projects/projects-eclipse.html#ReferencingLibraryProject

//General Java imports 
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

//Android imports 
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.*;
import android.widget.*;

//Import of resources file for file browser
import com.karnex.filedrop.R;
import com.karnex.filedrop.commons.Constants;

public class FileBrowserActivity extends Activity {
	// Intent Action Constants
	public static final String INTENT_ACTION_SELECT_DIR = "ua.com.vassiliev.androidfilebrowser.SELECT_DIRECTORY_ACTION";
	public static final String INTENT_ACTION_SELECT_FILE = "ua.com.vassiliev.androidfilebrowser.SELECT_FILE_ACTION";
	public static final String INTENT_ACTION_SELECT_MAIN = "com.karnex.filedrop.filemanager.MAIN_WINDOW_ACTION";

	// Intent parameters names constants
	public static final String startDirectoryParameter = "ua.com.vassiliev.androidfilebrowser.directoryPath";
	public static final String returnDirectoryParameter = "ua.com.vassiliev.androidfilebrowser.directoryPathRet";
	public static final String returnFileParameter = "ua.com.vassiliev.androidfilebrowser.filePathRet";
	public static final String showCannotReadParameter = "ua.com.vassiliev.androidfilebrowser.showCannotRead";
	public static final String filterExtension = "ua.com.vassiliev.androidfilebrowser.filterExtension";

	// Stores names of traversed directories
	ArrayList<String> pathDirsList = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	// private Boolean firstLvl = true;

	private static final String LOGTAG = "F_PATH";

	private List<Item> fileList = new ArrayList<Item>();
	private File path = null;
	private String chosenFile;
	private List<File> selList = new ArrayList<File>();
	// private static final int DIALOG_LOAD_FILE = 1000;

	ArrayAdapter<Item> adapter;
	
	// Buttons
	private Button importButton;
	private Button exportButton;
	private Button upDirButton;
	private Button newDirButton;
	private Button refreshDirButton;
	private Button cancelButton;
	private Button selectButton;
	private Button deleteButton;
	private Button homeButton;
	
	private String intentAction;

	private boolean showHiddenFilesAndDirs = true;

	private boolean directoryShownIsEmpty = false;
	
	private boolean selectMode = false;

	private String filterFileExtension = null;

	// Action constants
	private static int currentAction = -1;
	private static final int SELECT_DIRECTORY = 1;
	private static final int SELECT_FILE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// In case of
		// ua.com.vassiliev.androidfilebrowser.SELECT_DIRECTORY_ACTION
		// Expects com.mburman.fileexplore.directoryPath parameter to
		// point to the start folder.
		// If empty or null, will start from SDcard root.
		setContentView(R.layout.ua_com_vassiliev_filebrowser_layout);

		// Set action for this activity
		Intent thisInt = this.getIntent();
		currentAction = SELECT_FILE;// This would be a default action in
											// case not set by intent
		if (thisInt.getAction().equalsIgnoreCase(INTENT_ACTION_SELECT_DIR)) {
			Log.d(LOGTAG, "SELECT ACTION - SELECT FILE");
			currentAction = SELECT_DIRECTORY;
		}
		intentAction = thisInt.getAction();
		
		showHiddenFilesAndDirs = thisInt.getBooleanExtra(
				showCannotReadParameter, true);

		filterFileExtension = thisInt.getStringExtra(filterExtension);

		setInitialDirectory();

		parseDirectoryPath();
		loadFileList();
		this.createFileListAdapter();
		this.initializeButtons();
		this.setWindowView();
		this.initializeFileListView();
		updateCurrentDirectoryTextView();
		Log.d(LOGTAG, path.getAbsolutePath());
	}

	private void setInitialDirectory() {
		Intent thisInt = this.getIntent();
		String requestedStartDir = thisInt
				.getStringExtra(startDirectoryParameter);

		if (requestedStartDir != null && requestedStartDir.length() > 0) {// if(requestedStartDir!=null
			File tempFile = new File(requestedStartDir);
			if (tempFile.isDirectory())
				this.path = tempFile;
		}// if(requestedStartDir!=null

		if (this.path == null) {// No or invalid directory supplied in intent
								// parameter
			if (Environment.getExternalStorageDirectory().isDirectory()
					&& Environment.getExternalStorageDirectory().canRead())
				path = Environment.getExternalStorageDirectory();
			else
				path = new File("/");
		}// if(this.path==null) {//No or invalid directory supplied in intent
			// parameter
	}// private void setInitialDirectory() {

	private void parseDirectoryPath() {
		pathDirsList.clear();
		String pathString = path.getAbsolutePath();
		String[] parts = pathString.split("/");
		int i = 0;
		while (i < parts.length) {
			pathDirsList.add(parts[i]);
			i++;
		}
	}

	private void initializeButtons() {
		final Activity context = this;
		// Up Button
		upDirButton = (Button) this.findViewById(R.id.upDirectoryButton);
		upDirButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(LOGTAG, "onclick for upDirButton");
				loadDirectoryUp();
				loadFileList();
				adapter.notifyDataSetChanged();
				updateCurrentDirectoryTextView();
			}
		});// upDirButton.setOnClickListener(
		
		homeButton = (Button) this.findViewById(R.id.homeButton);
		homeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				path = new File(Constants.getAppDirectory());
				parseDirectoryPath();
				loadFileList();
				adapter.notifyDataSetChanged();
				updateCurrentDirectoryTextView();
			}
		});

		// New dir button
		newDirButton = (Button) this.findViewById(R.id.newDirectoryButton);
		newDirButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(LOGTAG, "onClick of newDirButton");
				AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle("New Folder");
				final EditText input = new EditText(context);
				alert.setView(input);
				
				alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String dirName = input.getText().toString();
						File newDir = new File(path.getAbsolutePath()+"/"+dirName);
						if(newDir.mkdir()) {
							showToast("Directory successfully created");
							refreshView();
						}
						else
							showToast("Falied to create directory");
					}
				});
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						return;
						
					}
				});
				alert.show();
			}
		});

		Button selectFolderButton = (Button) this
				.findViewById(R.id.selectCurrentDirectoryButton);
		if (false) {
			selectFolderButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.d(LOGTAG, "onclick for selectFolderButton");
					returnDirectoryFinishActivity();
				}
			});
		} else {// if(currentAction == this.SELECT_DIRECTORY) {
			selectFolderButton.setVisibility(View.GONE);
		}// } else {//if(currentAction == this.SELECT_DIRECTORY) {
		
		// Delete Button
		deleteButton = (Button) this.findViewById(R.id.deleteButton);
		deleteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				for(File sel:selList) {
					if(sel.canWrite()) {
						sel.delete();
					}
					else {
						showToast("Failed to delete "+sel.getName());
					}
				}
				cancelAction();
			}
		});
		
		// Refresh Button
		refreshDirButton = (Button) this.findViewById(R.id.refreshDirectoryButton);
		refreshDirButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				refreshView();
			}
		});
		
		// Cancel Button
		cancelButton = (Button) this.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(intentAction.equals(INTENT_ACTION_SELECT_DIR)) {
					finish();
				}
				else {
					cancelAction();
				}
			}
		});
		
		// Done Button
		//Button doneButton = (Button) this.findViewById(R.id.doneButton);
		
		// Import Button
		importButton = (Button) this.findViewById(R.id.importButton);
		importButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// If its importing into sub-directory of the Home directory, the set the import
				// path to that directory and open a new activity to get the files
				if(path.getAbsolutePath().startsWith(Constants.getAppDirectory())) {
					showToast("Not implemented for Home dir yet");
					//open file browser
				}
				else {
//					CopyTask copyTask = new CopyTask();
//					copyTask.destination = Constants.getAppDirectory();
//					copyTask.execute(selList);
					// Just copy the files to home directory
					for(File sel:selList) {
						Utils.copy(sel, Constants.getAppDirectory());
					}
					showToast("Import complete");
				}
				// Clean up
				cancelAction();
			}
		});
		
		// Export Button
		exportButton = (Button) this.findViewById(R.id.exportButton);
		exportButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FileManager.startFolderView(context, null);
			}
		});
		
		// Select Button
		selectButton = (Button) this.findViewById(R.id.selectButton);
		selectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(intentAction.equals(INTENT_ACTION_SELECT_DIR)) {
					returnDirectoryFinishActivity();
				} 
				else {
					selectButton.setEnabled(false);
					cancelButton.setEnabled(true);
					upDirButton.setEnabled(false);
					homeButton.setEnabled(false);
					selectMode = true;
				}
			}
		});
		
	}// private void initializeButtons() {
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FileManager.REQUEST_CODE_PICK_DIR) {
        	if(resultCode == RESULT_OK) {
        		String newDir = data.getStringExtra(
        				FileBrowserActivity.returnDirectoryParameter);
//        		Toast.makeText(
//        				this, 
//        				"Received DIRECTORY path from file browser:\n"+newDir, 
//        				Toast.LENGTH_LONG).show();
        		if(data.getStringExtra("Intent_Action").equals(INTENT_ACTION_SELECT_DIR)) { //Change this for export later
        			for(File sel:selList) {
        				Utils.copy(sel, newDir);
        			}
        			showToast("Export complete");
        		}
        		
	        	
        	} else {//if(resultCode == this.RESULT_OK) {
        		Toast.makeText(
        				this, 
        				"Received NO result from file browser",
        				Toast.LENGTH_LONG).show(); 
        		cancelAction();
        	}//END } else {//if(resultCode == this.RESULT_OK) {
        }//if (requestCode == REQUEST_CODE_PICK_DIR) {
		
		if (requestCode == FileManager.REQUEST_CODE_PICK_FILE) {
        	if(resultCode == RESULT_OK) {
        		String newFile = data.getStringExtra(
        				FileBrowserActivity.returnFileParameter);
        		Toast.makeText(
        				this, 
        				"Received FILE path from file browser:\n"+newFile, 
        				Toast.LENGTH_LONG).show(); 
	        	
        	} else {//if(resultCode == this.RESULT_OK) {
        		Toast.makeText(
        				this, 
        				"Received NO result from file browser",
        				Toast.LENGTH_LONG).show(); 
        	}//END } else {//if(resultCode == this.RESULT_OK) {
        }//if (requestCode == REQUEST_CODE_PICK_FILE) {
		
		
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void cancelAction() {
		refreshView();
		homeButton.setEnabled(true);
		deleteButton.setEnabled(false);
		importButton.setEnabled(false);
		exportButton.setEnabled(false);
		selectButton.setEnabled(true);
		cancelButton.setEnabled(false);
		upDirButton.setEnabled(true);
		selectMode = false;
		selList.clear();
	}

	private void loadDirectoryUp() {
		// present directory removed from list
		String s = pathDirsList.remove(pathDirsList.size() - 1);
		// path modified to exclude present directory
		path = new File(path.toString().substring(0,
				path.toString().lastIndexOf(s)));
		fileList.clear();
	}

	private void updateCurrentDirectoryTextView() {
		int i = 0;
		String curDirString = "";
		while (i < pathDirsList.size()) {
			curDirString += pathDirsList.get(i) + "/";
			i++;
		}
		if (pathDirsList.size() == 0) {
			((Button) this.findViewById(R.id.upDirectoryButton))
					.setEnabled(false);
			curDirString = "/";
		} else
			((Button) this.findViewById(R.id.upDirectoryButton))
					.setEnabled(true);
		
		updateButtons();
		
		long freeSpace = getFreeSpace(curDirString);
		String formattedSpaceString = formatBytes(freeSpace);
		if (freeSpace == 0) {
			Log.d(LOGTAG, "NO FREE SPACE");
			File currentDir = new File(curDirString);
			if (!currentDir.canWrite())
				formattedSpaceString = "NON Writable";
		}

		((Button) this.findViewById(R.id.selectCurrentDirectoryButton))
				.setText("Select\n[" + formattedSpaceString + "]");

		if(curDirString.startsWith(Constants.getAppDirectory())) {
			curDirString = "Home"+curDirString.substring(Constants.getAppDirectory().length());
		}
		((TextView) this.findViewById(R.id.currentDirectoryTextView))
				.setText("Current directory: " + curDirString);
	}// END private void updateCurrentDirectoryTextView() {
	
	private void updateButtons() {
		// New Dir button
		if (path.canWrite())
			newDirButton.setEnabled(true);
		else
			newDirButton.setEnabled(false);
		
		if(path.getAbsolutePath().startsWith(Constants.getAppDirectory())) { // Check if currently in app directory
			importButton.setEnabled(true);
		}
		else {
			importButton.setEnabled(false);
		}
		if(this.intentAction.equals(INTENT_ACTION_SELECT_DIR)) {
			if(path.getAbsolutePath().startsWith(Constants.getAppDirectory()) || !path.canWrite()) {
				selectButton.setEnabled(false);
			}
			else {
				selectButton.setEnabled(true);
			}
		}
	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	private void initializeFileListView() {
		ListView lView = (ListView) this.findViewById(R.id.fileListView);
		lView.setBackgroundColor(Color.LTGRAY);
		LinearLayout.LayoutParams lParam = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lParam.setMargins(15, 5, 15, 5);
		lView.setAdapter(this.adapter);
		lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//XXX
				chosenFile = fileList.get(position).file;
				File sel = new File(path + "/" + chosenFile);
				if(selectMode) {
					if(fileList.get(position).isSelected) { //If selected, deselect
						selList.remove(selList.indexOf(sel));
						fileList.get(position).isSelected = false;
						//Update icons
						fileList.get(position).icon = fileList.get(position).original_icon;
						adapter.notifyDataSetChanged();
					}
					else {
						fileList.get(position).isSelected = true;
						selList.add(sel);
						fileList.get(position).original_icon = fileList.get(position).icon;
						if(sel.isDirectory())
							fileList.get(position).icon = R.drawable.dir_selected;
						else
							fileList.get(position).icon = R.drawable.file_icon_selected;
						adapter.notifyDataSetChanged();
					}
					if(selList.size() > 0) { // Has items
						if(path.getAbsolutePath().startsWith(Constants.getAppDirectory())) {
							exportButton.setEnabled(true);
						}
						else {
							importButton.setEnabled(true);
						}
						deleteButton.setEnabled(true);
					}
					else {
						exportButton.setEnabled(false);
						importButton.setEnabled(false);
						deleteButton.setEnabled(false);
					}
				}
				else {
					Log.d(LOGTAG, "Clicked:" + chosenFile);
					if (sel.isDirectory()) {
						if (sel.canRead()) {
							// Adds chosen directory to list
							pathDirsList.add(chosenFile);
							path = new File(sel + "");
							Log.d(LOGTAG, "Just reloading the list");
							loadFileList();
							adapter.notifyDataSetChanged();
							updateCurrentDirectoryTextView();
							Log.d(LOGTAG, path.getAbsolutePath());
						} else {// if(sel.canRead()) {
							showToast("Path does not exist or cannot be read");
						}// } else {//if(sel.canRead()) {
					}// if (sel.isDirectory()) {
						// File picked or an empty directory message clicked
					else {// if (sel.isDirectory()) {
						Log.d(LOGTAG, "item clicked");
						if (!directoryShownIsEmpty) {
							Log.d(LOGTAG, "File selected:" + chosenFile);
							//returnFileFinishActivity(sel.getAbsolutePath());
							openFile(sel);
						}
					}// else {//if (sel.isDirectory()) {
				}// public void onClick(DialogInterface dialog, int which) {
			}
		});// lView.setOnClickListener(
	}// private void initializeFileListView() {
	
	private void openFile(File sel) {
		try {
			FileOpen.openFile(this, sel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setWindowView() {
		Intent intent = this.getIntent();
		if(intent.getAction().equalsIgnoreCase(INTENT_ACTION_SELECT_MAIN)) {
//			Button doneButton = (Button) this.findViewById(R.id.doneButton);
//			doneButton.setVisibility(View.INVISIBLE);
		}
		else if(intent.getAction().equalsIgnoreCase(INTENT_ACTION_SELECT_DIR)) {
			importButton.setVisibility(View.INVISIBLE);
			exportButton.setVisibility(View.INVISIBLE);
			selectButton.setEnabled(true);
			cancelButton.setEnabled(true);
		}
	}


	private void returnDirectoryFinishActivity() {
		Intent retIntent = new Intent();
		retIntent.putExtra(returnDirectoryParameter, path.getAbsolutePath());
		retIntent.putExtra("Intent_Action", intentAction);
		this.setResult(RESULT_OK, retIntent);
		this.finish();
	}// END private void returnDirectoryFinishActivity() {

	private void returnFileFinishActivity(String filePath) {
		Intent retIntent = new Intent();
		retIntent.putExtra(returnFileParameter, filePath);
		this.setResult(RESULT_OK, retIntent);
		this.finish();
	}// END private void returnDirectoryFinishActivity() {

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(LOGTAG, "unable to write on the sd card ");
		}
		fileList.clear();

		if (path.exists() && path.canRead()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					boolean showReadableFile = showHiddenFilesAndDirs
							|| sel.canRead();
					// Filters based on whether the file is hidden or not
					if (currentAction == SELECT_DIRECTORY) {
						return (sel.isDirectory() && showReadableFile);
					}
					if (currentAction == SELECT_FILE) {

						// If it is a file check the extension if provided
						if (sel.isFile() && filterFileExtension != null) {
							return (showReadableFile && sel.getName().endsWith(
									filterFileExtension));
						}
						return (showReadableFile);
					}
					return true;
				}// public boolean accept(File dir, String filename) {
			};// FilenameFilter filter = new FilenameFilter() {

			String[] fList = path.list(filter);
			this.directoryShownIsEmpty = false;
			for (int i = 0; i < fList.length; i++) {
				// Convert into file path
				File sel = new File(path, fList[i]);
//				Log.d(LOGTAG,
//						"File:" + fList[i] + " readable:"
//								+ (Boolean.valueOf(sel.canRead())).toString()
//								+ " writable:"
//								+ (Boolean.valueOf(sel.canWrite())).toString());
				int drawableID = R.drawable.file_icon;
				boolean canRead = sel.canRead();
				// Set drawables
				if (sel.isDirectory()) {
					if (canRead) {
						drawableID = R.drawable.folder_icon;
					} else {
						drawableID = R.drawable.folder_icon_light;
					}
				}
				fileList.add(i, new Item(fList[i], drawableID, canRead));
			}// for (int i = 0; i < fList.length; i++) {
			if (fileList.size() == 0) {
				// Log.d(LOGTAG, "This directory is empty");
				this.directoryShownIsEmpty = true;
				fileList.add(0, new Item("Directory is empty", -1, true));
			} else {// sort non empty list
				Collections.sort(fileList, new ItemFileNameComparator());
			}
		} else {
			Log.e(LOGTAG, "path does not exist or cannot be read");
		}
		// Log.d(TAG, "loadFileList finished");
	}// private void loadFileList() {

	private void createFileListAdapter() {
		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);
				// put the image on the text view
				int drawableID = 0;
				if (fileList.get(position).icon != -1) {
					// If icon == -1, then directory is empty
					drawableID = fileList.get(position).icon;
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(drawableID, 0,
						0, 0);

				textView.setEllipsize(null);

				// add margin between image and text (support various screen
				// densities)
				// int dp5 = (int) (5 *
				// getResources().getDisplayMetrics().density + 0.5f);
				int dp3 = (int) (3 * getResources().getDisplayMetrics().density + 0.5f);
				// TODO: change next line for empty directory, so text will be
				// centered
				textView.setCompoundDrawablePadding(dp3);
				textView.setBackgroundColor(Color.LTGRAY);
				return view;
			}// public View getView(int position, View convertView, ViewGroup
		};// adapter = new ArrayAdapter<Item>(this,
	}// private createFileListAdapter(){
	
	private void refreshView() {
		loadFileList();
		adapter.notifyDataSetChanged();
	}

	private class Item {
		public String file;
		public int icon;
		public int original_icon;
		public boolean canRead;
		public boolean isSelected;

		public Item(String file, Integer icon, boolean canRead) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}// END private class Item {
	
	private class CopyTask extends AsyncTask<List<File>, Void, Void> {
		protected String destination;
		@Override
		protected Void doInBackground(List<File>... params) {
			for(File sel:selList) {
				Utils.copy(sel, Constants.getAppDirectory());
			}
			return null;
		}
		
	}

	private class ItemFileNameComparator implements Comparator<Item> {
		public int compare(Item lhs, Item rhs) {
			return lhs.file.toLowerCase().compareTo(rhs.file.toLowerCase());
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Log.d(LOGTAG, "ORIENTATION_LANDSCAPE");
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			Log.d(LOGTAG, "ORIENTATION_PORTRAIT");
		}
		// Layout apparently changes itself, only have to provide good onMeasure
		// in custom components
		// TODO: check with keyboard
		// if(newConfig.keyboard == Configuration.KEYBOARDHIDDEN_YES)
	}// END public void onConfigurationChanged(Configuration newConfig) {

	public static long getFreeSpace(String path) {
		StatFs stat = new StatFs(path);
		long availSize = (long) stat.getAvailableBlocks()
				* (long) stat.getBlockSize();
		return availSize;
	}// END public static long getFreeSpace(String path) {

	public static String formatBytes(long bytes) {
		// TODO: add flag to which part is needed (e.g. GB, MB, KB or bytes)
		String retStr = "";
		// One binary gigabyte equals 1,073,741,824 bytes.
		if (bytes > 1073741824) {// Add GB
			long gbs = bytes / 1073741824;
			retStr += (new Long(gbs)).toString() + "GB ";
			bytes = bytes - (gbs * 1073741824);
		}
		// One MB - 1048576 bytes
		if (bytes > 1048576) {// Add GB
			long mbs = bytes / 1048576;
			retStr += (new Long(mbs)).toString() + "MB ";
			bytes = bytes - (mbs * 1048576);
		}
		if (bytes > 1024) {
			long kbs = bytes / 1024;
			retStr += (new Long(kbs)).toString() + "KB";
			bytes = bytes - (kbs * 1024);
		} else
			retStr += (new Long(bytes)).toString() + " bytes";
		return retStr;
	}// public static String formatBytes(long bytes){

}// END public class FileBrowserActivity extends Activity {
