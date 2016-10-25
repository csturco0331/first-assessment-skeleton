package com.cooksys.assessment.game;

import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;

public class TicTacToe {
	private Logger log = LoggerFactory.getLogger(TicTacToe.class);				//log info
	private char[] xos = {' ',' ',' ',' ',' ',' ',' ',' ',' '};
	private Player one;
	private Player two;
	private boolean playerOnesMove;
	private boolean finished = false;
	final private String password;
	private int moveCount = 0;
	
	public TicTacToe(String password, Player one) {
		this.password = password;
		this.one = one;
		playerOnesMove = true;
	}
	
	public void setSecondPlayer(Player two) {
		this.two = two;
	}
	
	public Player getSecondPlayer() {
		return two;
	}
	
	public Player getFIrstPlayer() {
		return one;
	}
	
	public void play(Message message) {
		if(playerOnesMove == (message.getUsername().equals(one.getUsername())))
			move(Integer.parseInt(message.getContents().split(" ")[1]));
		else if (message.getUsername().equals(one.getUsername())) {
			one.getMessageQueue().add(new Message(message.getUsername(), "tmove", "it is not your move", new Timestamp(System.currentTimeMillis()).toString()));
			synchronized (one.getMessageQueue()) {
				one.getMessageQueue().notify();																		//notify the users ClientQueue
			}
		} else {
			two.getMessageQueue().add(new Message(message.getUsername(), "tmove", "it is not your move", new Timestamp(System.currentTimeMillis()).toString()));
			synchronized (two.getMessageQueue()) {
				two.getMessageQueue().notify();																		//notify the users ClientQueue
			}
		}
	}
	
	private void move(int index) {
		if(xos[index] != ' ') {
			if(playerOnesMove) {
				one.getMessageQueue().add(new Message(one.getUsername(), "tmove", "illegal move", new Timestamp(System.currentTimeMillis()).toString()));
				synchronized (one.getMessageQueue()) {
					one.getMessageQueue().notify();																		//notify the users ClientQueue
				}
			}
			else {
				two.getMessageQueue().add(new Message(two.getUsername(), "tmove", "illegal move", new Timestamp(System.currentTimeMillis()).toString()));
				synchronized (two.getMessageQueue()) {
					two.getMessageQueue().notify();																		//notify the users ClientQueue
				}
			}
		}
		if(playerOnesMove) xos[index] = 'X';
		else xos[index] = 'O';
		moveCount++;
		String content = checkWin();
		playerOnesMove = !playerOnesMove;
		content = "\n_____________\n"
			 + "| "+xos[0]+" | "+xos[1]+" | "+xos[2]+" |\n"
			 + "| "+xos[3]+" | "+xos[4]+" | "+xos[5]+" |\n"
			 + "| "+xos[6]+" | "+xos[7]+" | "+xos[8]+" |\n"
			 + "-------------\n" + content;
		Message result = new Message(two.getUsername(), "tmove", content, new Timestamp(System.currentTimeMillis()).toString());
		log.info("sending result");
		one.getMessageQueue().add(result);
		synchronized (one.getMessageQueue()) {
			one.getMessageQueue().notify();																		//notify the users ClientQueue
		}
		two.getMessageQueue().add(result);
		synchronized (two.getMessageQueue()) {
			two.getMessageQueue().notify();																		//notify the users ClientQueue
		}
	}
	
	private String checkWin() {
		if( (xos[0] == xos[1] && xos[0] == xos[2] && xos[0] != ' ')||
			(xos[3] == xos[4] && xos[3] == xos[5] && xos[3] != ' ')||	
			(xos[6] == xos[7] && xos[6] == xos[8] && xos[6] != ' ')||
			(xos[0] == xos[3] && xos[0] == xos[6] && xos[0] != ' ')||
			(xos[1] == xos[4] && xos[1] == xos[7] && xos[1] != ' ')||
			(xos[2] == xos[5] && xos[2] == xos[8] && xos[2] != ' ')||
			(xos[0] == xos[4] && xos[0] == xos[8] && xos[0] != ' ')||
			(xos[2] == xos[4] && xos[2] == xos[6] && xos[2] != ' ')
		  ) {
			finished = true;
			return playerOnesMove ? one.getUsername() + " wins!\n" : two.getUsername() + " wins!";
		}
		if(moveCount == 9) {
			finished = true;
			return "Tied Game";
		}
		return "";
	}
	
	public boolean comparePassword(String input) {
		return input.equals(password);
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	
}
