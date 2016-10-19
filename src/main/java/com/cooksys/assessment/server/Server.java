package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);										//logs info
	private Map<String, Queue<Message>> users = new ConcurrentHashMap<String, Queue<Message>>();	//Single Map sent to all created clienthandlers to allow communication between clients
	private int port;																				//port that server is listening on
	private ExecutorService executor;																//used to create and manage the thread
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
	}

	public void run() {
		log.info("server started");																	//log server start
		ServerSocket ss;																			//server socket to handle listening for clients
		try {
			ss = new ServerSocket(this.port);														//initializing the server socket to listen on the specified port
			while (true) {																			//continually loop
				Socket socket = ss.accept();														//create a socket each time a client connects
				ClientHandler handler = new ClientHandler(socket, users);							//create a new ClientHandler
				executor.execute(handler);															//start a new thread for the ClientHandler
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);												//log error
		}
	}

}
