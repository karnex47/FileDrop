package com.karnex.filedrop.scheduler;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

import com.karnex.filedrop.commons.Command;
import com.karnex.filedrop.commons.FileItem;

public class Scheduler {
	private static Queue<com.karnex.filedrop.commons.FileItem> queue = new LinkedList<FileItem>();
	
	public static void putInQueue(String event, File entry) {
		if(entry.getName() == ".DS_Store") return;
		
		FileItem fileItem;
		if(event.equals("ENTRY_CREATE") || event.equals("ENTRY_MODIFY")) {
			if(entry.isDirectory()) {
				fileItem = new FileItem(String.valueOf(Command.mkdir), entry.getAbsolutePath());
				queue.add(fileItem);
				for(File sub:entry.listFiles()) {
					putInQueue("ENTRY_CREATE", sub);
				}
			}
			else {
				Log.d("Command.cp.getCommand()",String.valueOf(Command.cp));
				fileItem = new FileItem(String.valueOf(Command.cp), entry.getAbsolutePath(), entry.length());
				queue.add(fileItem);
			}
		}
//		else if (event.equals("ENTRY_MODIFY")) {
//			fileItem = new FileItem(String.valueOf(Command.rm), entry.getAbsolutePath());
//			queue.add(fileItem);
//			fileItem = new FileItem(String.valueOf(Command.cp), entry.getAbsolutePath());
//			queue.add(fileItem);
//		}
		else if (event.equals("ENTRY_DELETE")) {
			fileItem = new FileItem(String.valueOf(Command.rm), entry.getAbsolutePath());
			queue.add(fileItem);
		}
	}
	
	public static FileItem pollFromQueue() {
		return queue.poll();
	}
	
	public static FileItem peekQueue() {
		return queue.peek();
	}
}
