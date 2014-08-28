package com.karnex.commons;

public class FileItem {

	private String name;
	private Command cmd;
	private long fileSize;
	
	public FileItem(Command cmd, String name, long fileSize) {
		super();
		this.name = name;
		this.cmd = cmd;
		this.fileSize = fileSize;
	}

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
	
	public FileItem(Command c, String s){
		setName(s);
		setCmd(c);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Command getCmd() {
		return cmd;
	}

	public void setCmd(Command cmd) {
		this.cmd = cmd;
	}
	
	@Override
	public String toString() {
		String ret = "";
		ret += cmd.toString();
		if(name != null || name.length() > 0) {
			ret+="|"+name.substring(Config.getHomePath().length());
		}
		ret = ret.replace("\\", "/");
		if(cmd == Command.cp) {
			ret+= "|"+this.getFileSize();
		}
		return ret;
	}
	
	public static FileItem parse(String input) {
		String[] tokens = input.split("|");
		FileItem ret = new FileItem();
		ret.setCmd(Command.valueOf(tokens[0]));
		if(tokens[1] != null)
			ret.setName(tokens[1]);
		return ret;
	}
}
