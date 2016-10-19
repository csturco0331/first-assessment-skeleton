package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	static Map<String, Queue<Message>> users = Collections.synchronizedMap(new HashMap<String, Queue<Message>>());
	private Socket socket;
	private Queue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();
	Boolean running = true;
	
	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (running) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				String response;
				Queue<Message> temporaryQueue;
				switch (message.getCommand()) {
					case "connect":						
						if(users.containsKey(message.getUsername())){
							log.info("username <{}> already taken", message.getUsername());
							response = mapper.writeValueAsString(new Message());
							writer.write(response);
							writer.flush();
							this.socket.close();
							break;
						} else {
							new Thread(new ClientQueue(messageQueue, socket, running, writer)).start();
							users.put(message.getUsername(), messageQueue);
							log.info("user <{}> connected", message.getUsername());
							for(String key : users.keySet()) {
								log.info(key + "test");
								temporaryQueue = users.get(key);
								temporaryQueue.add(new Message(message.getUsername(), "connection", ""));
								synchronized (temporaryQueue) {
									temporaryQueue.notify();
								}
							}
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						users.remove(message.getUsername());
						running = false;
						for(String key : users.keySet()) {
							temporaryQueue = users.get(key);
							temporaryQueue.add(new Message(message.getUsername(), "disconnection", ""));
							synchronized (temporaryQueue) {
								temporaryQueue.notify();
							}
						}
						synchronized(messageQueue) {
							messageQueue.notify();
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						messageQueue.add(message);
						synchronized(messageQueue) {
							messageQueue.notify();
						}
						break;
					case "broadcast":
						log.info("user <{}> sent message <{}> to all users", message.getUsername(), message.getCommand());
						for(String key : users.keySet()) {
							temporaryQueue = users.get(key);
							temporaryQueue.add(message);
							synchronized (temporaryQueue) {
								temporaryQueue.notify();
							}
						}
						break;
					case "users":
						log.info("user <{}> requested users", message.getUsername());
						Set<String> keys = users.keySet();
						String content = "";
						for(String user : keys) {
							content += "<" + user + "> ";
						}
						messageQueue.add(new Message(message.getUsername(), message.getCommand(), content));
						synchronized(messageQueue) {
							messageQueue.notify();
						}
						break;
					case "@":
						String key = message.getContents().split(" ")[0];
						String contents = message.getContents().replaceFirst(key+" ", "");
						log.info("user <{}> sent message <{}> to user <{}>", message.getUsername(), key, contents);
						message.setContents(contents);
						temporaryQueue = users.get(key);
						temporaryQueue.add(message);
						synchronized(temporaryQueue) {
							temporaryQueue.notify();
						}
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
