package com.cooksys.assessment.model;

public class Message {

	private String username;
	private String command;
	private String contents;
	private String timestamp;

	public Message() {
		
	}
	
	public Message(String username, String command, String contents, String timestamp) {
		this.username = username;
		this.command = command;
		this.contents = contents;
		this.timestamp = timestamp;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

}
