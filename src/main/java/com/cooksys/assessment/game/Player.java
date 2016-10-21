package com.cooksys.assessment.game;

import java.util.Queue;

import com.cooksys.assessment.model.Message;

public class Player {

	private String username;
	private Queue<Message> messageQueue;
	
	public Player(String username, Queue<Message> messageQueue) {
		this.username = username;
		this.messageQueue = messageQueue;
	}
	
	public String getUsername() {
		return username;
	}
	
	public Queue<Message> getMessageQueue() {
		return messageQueue;
	}
	
}
