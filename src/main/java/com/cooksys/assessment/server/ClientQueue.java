package com.cooksys.assessment.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientQueue implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientQueue.class);				//log info
	private Queue<Message> messageQueue;											//users message queue brought in from the creating clienthandler																	
	PrintWriter writer;																//buffer for writing to the client
	Boolean running;																//running boolean passed in from the clienthandler so the handler can control the stop of this thread

    //=======================CONSTRUCTOR===============================
	public ClientQueue(Queue<Message> messageQueue, Boolean running, PrintWriter writer) {
		this.messageQueue = messageQueue;
		this.running = running;
		this.writer = writer;
	}

	//=======================RUN=======================================
	@Override
	public void run() {
		String response;																//temporary String variable
		ObjectMapper mapper = new ObjectMapper();										//converts strings to objects and vise-versa
		try {
			while (running) {															//while running is true (i.e. handler hasn't stopped)
				while (messageQueue.isEmpty()) {										//while the queue is empty we will wait() on the messageQueue
					synchronized (messageQueue) {
						try {
							messageQueue.wait();										//wait for messageQueue to get a notify()
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if(!running)													//check if handler stopped us while waiting.
							throw new Exception();										//if so, throw exception to break out of while loops
					}
				}
				response = mapper.writeValueAsString(messageQueue.remove());			//get message out of the queue and parse to string
				writer.write(response);													//write the response to the buffer
				writer.flush();															//flush buffer to write to client
			}
		} catch (IOException e) {														//catch IO errors
			e.printStackTrace();														//and print to stack
		} catch (Exception e) {															//catch error thrown by stopped running
			
		} finally {
			log.info("closing");														//log closing
			writer.close();																//close writer
		}

	}

}
