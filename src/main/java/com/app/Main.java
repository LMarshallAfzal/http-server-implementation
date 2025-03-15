package com.app;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Main class is the entry point for the HTTP server application.
 * It creates an  Acceptor to listen for incoming connections and handles
 * client requests by creating a new thread for each connection.
 * 
 * <p>The server runs in an infinite loop, continuously accepting new connections
 * and processing requests concurrently.</p>
 */
public class Main {
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
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

        try {
            Acceptor acceptor = new Acceptor(enableSSL);
            ConnectionManager connectionManager = new ConnectionManager();
            System.out.println("Server started " + (enableSSL ? "with SSL" : "without SSL"));

            while (true) {
                Socket clientSocket = acceptor.acceptConnections(connectionManager);

                int threadId = threadCounter.incrementAndGet();
                System.out.println("Creating thread #" + threadId + " - Total active threads: " + threadCounter.get());

                if (!clientSocket.isClosed()) {
                    new Thread(() -> {
                        try {
                            System.out.println("Thread #" + threadId + " started\n");

                            clientSocket.setSoTimeout(30000);
                            Processor processor = new Processor();
                            Responder responder = new Responder();

                            PushbackInputStream pbInputStream = new PushbackInputStream(
                                    clientSocket.getInputStream(), 1
                            );
                            OutputStream outputStream = clientSocket.getOutputStream();

                            while (!clientSocket.isClosed()) {
                                try {
                                    int peekedByte = pbInputStream.read();
                                    if (peekedByte != -1) {
                                        pbInputStream.unread(peekedByte);

                                        HttpRequest request = processor.parseRequest(pbInputStream);
                                        HttpResponse response = processor.processRequest(request);
                                        responder.sendResponse(response, outputStream);

                                        boolean keepAlive = request.getRequestHeaders().containsKey("Connection") && request.getRequestHeaders().get("Connection").equals("keep-alive");

                                        if (!keepAlive) {
                                            break;
                                        }
                                    }
                                } catch (SocketTimeoutException e) {
                                    System.out.println("Connection to " + clientSocket.getInetAddress().getHostName() + " timed out");
                                    break;
                                } catch (IOException e) {
                                    System.out.println("IO error processing request: " + e.getMessage());
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                System.out.println("Thread #" + threadId + " terminated");
                                threadCounter.decrementAndGet();
                                System.out.println("Remaining active threads: " + threadCounter.get());
                                connectionManager.removeClient(clientSocket.getInetAddress().getHostAddress());
                                clientSocket.close();
                            } catch (IOException closeEx) {
                                closeEx.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
