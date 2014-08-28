package com.karnex.filedrop.connection;

import android.os.AsyncTask;

public class BluetoothBackgroundTask extends AsyncTask<Void, Void, Void>{

	private Console console;
	@Override
	protected Void doInBackground(Void... params) {
		console.execute();
		while(!isCancelled()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		console.stopThreads();
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		
	}
	
	public Console getConsole() {
		return console;
	}
	public void setConsole(Console console) {
		this.console = console;
	}

}
