package com.karnex.filedrop.commons;

import java.util.StringTokenizer;

import android.util.Log;


public class FileItem {

	private String name;
	private String cmd;
	private long fileSize;
	
	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public FileItem(){
		setName("");
		setCmd(null);
	}
	
	public FileItem(String name, String cmd, long fileSize) {
		super();
		this.name = name;
		this.cmd = cmd;
		this.fileSize = fileSize;
	}

	public FileItem(String c, String s){
		setName(s);
		setCmd(c);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String string) {
		this.cmd = string;
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += cmd.toString();
		if(name != null || name.length() > 0) {
			ret+="|"+name.substring(Config.getHomePath().length()+1);
		}
		ret = ret.replace("/", "\\");
		return ret;
	}
	
	public static FileItem parse(String input) 
	{
		FileItem ret = new FileItem();
		StringTokenizer tokens = new StringTokenizer(input, "|");
		if(tokens.countTokens() > 0) {
			ret.setCmd(tokens.nextToken());
			if(tokens.hasMoreElements()){
				ret.setName(Constants.getAppDirectory()+tokens.nextToken());
			}
			if(tokens.hasMoreTokens() && ret.getCmd().equals("cp")) {
				ret.setFileSize(Long.parseLong(tokens.nextToken()));
			}
			return ret;
		}
		return null;
	}
}
