package com.karnex.filedrop.filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class Utils {
	private static final String LOGTAG = "Utils";
	
	public static void copy(File src, String dst) {
		if(src.canRead()) {
			Log.d(LOGTAG, "src:"+src.getAbsolutePath()+" dst:"+dst);
			if(dst.endsWith("/")) {
				dst = dst+src.getName();
			}
			else {
				dst = dst+"/"+src.getName();
			}
			if(src.isDirectory()) {
				new File(dst).mkdirs();
				for(File sub:src.listFiles()) {
					Utils.copy(sub, dst);
				}
			}
			else {
				File dstFile = new File(dst);
				try {
					InputStream inputStream = new FileInputStream(src);
					OutputStream outputStream = new FileOutputStream(dstFile);
					
					byte[] buffer = new byte[1024];
					int length;
					while((length = inputStream.read(buffer))>0) {
						outputStream.write(buffer, 0, length);
					}
					inputStream.close();
					outputStream.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
}
