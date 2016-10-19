package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	
	private final String CONNECT = "connect";
	private final String DISCONNECT = "disconnect";
	private final String ECHO = "echo";
	private final String BROADCAST = "broadcast";
	private final String USERS = "users";
	private final String AT = "@";
	private final String CONNECTION = "connection";
	private final String DISCONNECTION = "disconnection";
	private final String TAKEN = "taken";
	
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);				//logs information
	private Map<String, Queue<Message>> users;										//map from server that keeps track of all connected users and their respective queues
	private Socket socket;															//socket for connection
	private Queue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();		//individual users queue for storing messages and enforcing concurrency
	Boolean running = true;															//running flag for while loop, specifically useful for stopping the ClientQueue thread
	String currentUser;																//username of the current user

	//================CONSTRUCTOR=============================
	public ClientHandler(Socket socket, Map<String, Queue<Message>> users) {
		super();
		this.users = users;
		this.socket = socket;
	}

	//================SENDALL===============================
	//method for commands that send a message to all the connected users
	private void sendAll(Message message) {
		Queue<Message> temporaryQueue;															//temporary Queue
		for (String key : users.keySet()) {														//iterate over all users in the Map
			temporaryQueue = users.get(key);													//get each users messagequeue
			temporaryQueue.add(message); //add a new disconnection message to their queue
			synchronized (temporaryQueue) {
				temporaryQueue.notify();														//notify the queue to wake up waiting ClientQueue thread
			}
		}
	}
	
	//================RUN=====================================
	@Override //from interface Runnable
	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();														//takes a string representation of an object and creates the object
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));		//reads messages from client
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));			//writes messages to client

			while (running) {
				String raw = reader.readLine();																//reads buffer to get String form of Message from client
				Message message = mapper.readValue(raw, Message.class);										//converts the string into the Message
				message.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());					//adds a timestamp to the message
				String response;																			//temporary string variable : may be changed to fit needs
				//Handles messages on a case basis dependent on the Command
				switch (message.getCommand()) {
				case CONNECT:
					if (users.containsKey(message.getUsername())) {											//IF: check if the username requested is taken
						log.info("username <{}> already taken", message.getUsername());						//log result
						response = mapper.writeValueAsString(new Message(message.getUsername(), TAKEN, "", new Timestamp(System.currentTimeMillis()).toString())); //create taken message for client as a string
						writer.write(response);																//write the message to the write buffer
						writer.flush();																		//flush the buffer, sending the message to the client
						this.socket.close();																//close socket
						break;
					} else {																				//ELSE: username requested is available
						new Thread(new ClientQueue(messageQueue, running, writer)).start();			//create a new thread of clientqueue that handles the buffering of messages as well as writing to the client
						users.put(message.getUsername(), messageQueue);										//add the username and this objects messageQueue to the users map so other users can see him
						currentUser = message.getUsername();												//set currentUser
						log.info("user <{}> connected", message.getUsername());								//log result
						sendAll(new Message(message.getUsername(), CONNECTION, "", new Timestamp(System.currentTimeMillis()).toString()));//Notify all users that a new user has connected
					}
					break;
				case DISCONNECT:
					log.info("user <{}> disconnected", message.getUsername());								//log disconnect
					users.remove(message.getUsername());													//remove user from the map
					running = false;																		//set running to false, will stop the looping of this thread and ClientQueue
					sendAll(new Message(message.getUsername(), DISCONNECTION, "", new Timestamp(System.currentTimeMillis()).toString())); //Notify all users that a user has disconnected
					synchronized (messageQueue) {
						messageQueue.notify();																//wake up users
					}
					this.socket.close();																	//close socket
					break;
				case ECHO:
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());//log echo	
					messageQueue.add(message);																//add message to users queue
					synchronized (messageQueue) {
						messageQueue.notify();																//notify the queue to wake up waiting ClientQueue thread
					}
					break;
				case BROADCAST:
					log.info("user <{}> sent message <{}> to all users", message.getUsername(), message.getCommand());	//log broadcast
					sendAll(message);																		//Notify all users that a user has disconnected
					break;
				case USERS:
					log.info("user <{}> requested users", message.getUsername());							//log users
					Set<String> keys = users.keySet();														//get all the users from the map
					String content = "";																	//start of string
					for (String user : keys) {																//for each user
						content += "\n<" + user + ">";														//add user to string
					}
					messageQueue.add(new Message(message.getUsername(), message.getCommand(), content, new Timestamp(System.currentTimeMillis()).toString())); //send message to users queue
					synchronized (messageQueue) {
						messageQueue.notify();																//notify the users ClientQueue
					}
					break;
				case AT:
					String key = message.getContents().split(" ")[0];										//remove intended user from the message contents
					if(!users.containsKey(key)) break;														//if intended user isn't connected, break
					String contents = message.getContents().replaceFirst(key + " ", "");					//remove the intended username from the message contents
					log.info("user <{}> sent message <{}> to user <{}>", message.getUsername(), key, contents); //log whisper
					message.setContents(contents);															//change message contents to new contents
					Queue<Message> temporaryQueue = users.get(key);											//get intended user's queue from the map
					temporaryQueue.add(message);															//add the message to the intended user's queue
					synchronized (temporaryQueue) {
						temporaryQueue.notify();															//notify intended users ClientQueue
					}
					break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);														//log error
			running = false;																				//set running to false to stop ClientQueue thread
			users.remove(currentUser);																		//remove the user from the server map
			synchronized (messageQueue) {
				messageQueue.notify();																		//notify the users ClientQueue
			}
		}
	}
}
