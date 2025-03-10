package com.app;

import java.io.IOException;
import java.net.Socket;

/**
 * The com.app.Main class is the entry point for the HTTP server application.
 * It creates a com.app.Acceptor to listen for incoming connections and handles
 * client requests by creating a new thread for each connection.
 * 
 * <p>The server runs in an infinite loop, continuously accepting new connections
 * and processing requests concurrently.</p>
 */
public class Main {
    
    /**
     * The main method that starts the HTTP server.
     * 
     * <p>It creates a com.app.Acceptor to listen on port 8080 and enters an infinite loop
     * to accept incoming connections. For each connection, it creates a new thread
     * that processes the request and sends a response.</p>
     * 
     * @param args command line arguments (not used)
     * @throws IOException if an I/O error occurs when creating the com.app.Acceptor
     */
    public static void main(String[] args) throws IOException {
        boolean enableSSL = args.length > 0 && args[0].equalsIgnoreCase("--ssl");

        try {
            Acceptor acceptor = new Acceptor(enableSSL);
            System.out.println("Server started " + (enableSSL ? "with SSL" : "without SSL"));

            while (true) {
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
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
