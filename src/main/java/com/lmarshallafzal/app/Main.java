package com.lmarshallafzal.app;
import java.io.IOException;
import java.net.Socket;

import src.main.java.Acceptor;
import src.main.java.HttpResponse;
import src.main.java.Processor;
import src.main.java.Responder;

/**
 * The Main class is the entry point for the HTTP server application.
 * It creates an Acceptor to listen for incoming connections and handles
 * client requests by creating a new thread for each connection.
 * 
 * <p>The server runs in an infinite loop, continuously accepting new connections
 * and processing requests concurrently.</p>
 */
public class Main {
    
    /**
     * The main method that starts the HTTP server.
     * 
     * <p>It creates an Acceptor to listen on port 8080 and enters an infinite loop
     * to accept incoming connections. For each connection, it creates a new thread
     * that processes the request and sends a response.</p>
     * 
     * @param args command line arguments (not used)
     * @throws IOException if an I/O error occurs when creating the Acceptor
     */
    public static void main(String[] args) throws IOException {
        Acceptor acceptor = new Acceptor();

        while(true) {
            Socket clientSocket = acceptor.acceptConnections();

            new Thread(() -> {
                try {
                    Processor processor = new Processor();
                    HttpResponse response = processor.processRequest(processor.parseRequest(clientSocket.getInputStream()));
                    
                    Responder responder = new Responder();
                    responder.sendResponse(response, clientSocket.getOutputStream());

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    
}
