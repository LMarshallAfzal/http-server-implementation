package com.app;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;

/**
 * The Main class is the entry point for the HTTP server application.
 * It creates an  Acceptor to listen for incoming connections and handles
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
     * @param args command line arguments '--ssl' for encryption
     * @throws IOException if an I/O error occurs when creating the Acceptor
     */
    public static void main(String[] args) throws IOException {
        boolean enableSSL = args.length > 0 && args[0].equalsIgnoreCase("--ssl");
//        ArrayList<Socket> connectedClients = new ArrayList<>();

        try {
            Acceptor acceptor = new Acceptor(enableSSL);
            System.out.println("Server started " + (enableSSL ? "with SSL" : "without SSL"));

            while (true) {
//                connectedClients.add(acceptor.acceptConnections());
                Socket clientSocket = acceptor.acceptConnections();

                new Thread(() -> {
                    try {
                        Processor processor = new Processor();
                        HttpRequest request = processor.parseRequest(clientSocket.getInputStream());

                        HttpResponse response = processor.processRequest(request);

                        Responder responder = new Responder();
                        responder.sendResponse(response, clientSocket.getOutputStream());

                        boolean keepAlive = request.getRequestHeaders().containsKey("Connection") && request.getRequestHeaders().get("Connection").equals("keep-alive");

                        if (!keepAlive) {
                            clientSocket.close();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            clientSocket.close();
                        } catch (IOException closeEx) {
                            closeEx.printStackTrace();
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
