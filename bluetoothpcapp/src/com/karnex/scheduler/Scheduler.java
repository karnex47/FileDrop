package com.karnex.scheduler;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import com.karnex.commons.Command;
import com.karnex.commons.FileItem;

public class Scheduler {
	private static Queue<FileItem> queue = new LinkedList<FileItem>();
	
	public static void putInQueue(String event, File entry) {
		if(entry.getName() == ".DS_Store") return;
		
		FileItem fileItem;
		if(event.equals("ENTRY_CREATE")) {
			if(entry.isDirectory()) {
				fileItem = new FileItem(Command.mkdir, entry.getAbsolutePath());
				queue.add(fileItem);
				for(File sub:entry.listFiles()) {
					putInQueue("ENTRY_CREATE", sub);
				}
			}
			else {
				fileItem = new FileItem(Command.cp, entry.getAbsolutePath(), entry.length());
				queue.add(fileItem);
			}
		}
//		else if (event.equals("ENTRY_MODIFY")) {
//			fileItem = new FileItem(Command.rm, entry.getAbsolutePath());
//			queue.add(fileItem);
//			fileItem = new FileItem(Command.cp, entry.getAbsolutePath());
//			queue.add(fileItem);
//		}
		else if (event.equals("ENTRY_DELETE")) {
			fileItem = new FileItem(Command.rm, entry.getAbsolutePath());
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
