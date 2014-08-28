package com.karnex.filedrop;

import com.karnex.filedrop.commons.Config;
import com.karnex.filedrop.commons.Constants;
import com.karnex.filedrop.connection.BluetoothDeviceList;
import com.karnex.filedrop.connection.Connect;
import com.karnex.filedrop.connection.Console;
import com.karnex.filedrop.filemanager.FileManager;
import com.karnex.filedrop.filemanager.FilemanagerMainActivity;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		Config.context = getApplicationContext();
		
		if(Constants.getAppDirectory() == null) {
			Constants.setAppDirectory(getExternalCacheDir().getAbsolutePath()); // To be replaced with settings later
		}
		final Activity context = this;
		Button fileManager = (Button) this.findViewById(R.id.button1);
		fileManager.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FileManager.startMainWindowActivity(context);
			}
		});
		
		Button btManager = (Button) this.findViewById(R.id.button2);
		btManager.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, BluetoothDeviceList.class);
				context.startActivity(intent);
			}
		});
		
		Button connTest = (Button) this.findViewById(R.id.button3);
		connTest.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, Connect.class);
				context.startActivity(intent);
			}
		});
		
		
		ToggleButton onOff = (ToggleButton) this.findViewById(R.id.toggleButton1);
		onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked) {
		        	Config.currConnection = new Console(getApplicationContext());
		        } else {
		     
		    			Config.currConnection.stopThreads();
		    		}
		        }
		});
		//FileManager.startMainWindowActivity(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
 
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
