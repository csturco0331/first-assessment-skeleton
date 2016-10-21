package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.game.TicTacToe;
import com.cooksys.assessment.model.Message;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);										//logs info
	private Map<String, Queue<Message>> users = new ConcurrentHashMap<String, Queue<Message>>();	//Single Map sent to all created clienthandlers to allow communication between clients
	private Set<TicTacToe> tictactoe = new HashSet<TicTacToe>();									//Set of tictactoe games
	private int port;																				//port that server is listening on
	private ExecutorService executor;																//used to create and manage the thread
	
	/**
	 * Server constructor
	 * @param port
	 * 			the port that the server will be listening for clients on
	 * @param executor
	 * 			the executor will be used to create the clienthandler threads
	 */
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
	}

	/**
	 * The server creates a ServerSocket that constantly listens for new
	 * clients, creating a Clienthandler for each client and spinning it
	 * off into a new thread, then starts listening for the next client
	 */
	public void run() {
		log.info("server started");																	//log server start
		ServerSocket ss;																			//server socket to handle listening for clients
		try {
			ss = new ServerSocket(this.port);														//initializing the server socket to listen on the specified port
			while (true) {																			//continually loop
				Socket socket = ss.accept();														//create a socket each time a client connects
				ClientHandler handler = new ClientHandler(socket, users, tictactoe);				//create a new ClientHandler
				executor.execute(handler);															//start a new thread for the ClientHandler
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);												//log error
		}
	}

}
