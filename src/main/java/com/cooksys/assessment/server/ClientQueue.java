package com.cooksys.assessment.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientQueue implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientQueue.class);
	private Queue<Message> messageQueue;
	Socket socket;
	PrintWriter writer;
	Boolean running;

	public ClientQueue(Queue<Message> messageQueue, Socket socket, Boolean running, PrintWriter writer)
			throws IOException {
		this.messageQueue = messageQueue;
		this.socket = socket;
		this.running = running;
		this.writer = writer;
	}

	@Override
	public void run() {
		String response;
		ObjectMapper mapper = new ObjectMapper();
		try {
			while (running) {
				while (messageQueue.isEmpty()) {
					synchronized (messageQueue) {
						try {
							messageQueue.wait();
							log.info(messageQueue.peek().getCommand());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if(!running)
							throw new Exception();
					}
				}
				response = mapper.writeValueAsString(messageQueue.remove());
				writer.write(response);
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			
		} finally {
			writer.close();
			try {
				this.socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
