package com.app;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Main class is the entry point for the HTTP server application.
 * It creates an Acceptor to listen for incoming connections and handles
 * client requests by creating a new thread for each connection.
 * 
 * <p>
 * The server runs in an infinite loop, continuously accepting new connections
 * and processing requests concurrently.
 * </p>
 */
public class Main {
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * The main method that starts the HTTP server.
     * 
     * <p>
     * It creates a com.app.Acceptor to listen on port 8080 and enters an infinite
     * loop
     * to accept incoming connections. For each connection, it creates a new thread
     * that processes the request and sends a response.
     * </p>
     * 
     * @param args command line arguments '--ssl' for encryption
     * @throws IOException if an I/O error occurs when creating the Acceptor
     */
    public static void main(String[] args) throws IOException {
        boolean enableSSL = args.length > 0 && args[0].equalsIgnoreCase("--ssl");

        try {
            Acceptor acceptor = new Acceptor(enableSSL);
            ConnectionManager connectionManager = new ConnectionManager();
            Http2ConnectionManager http2ConnectionManager = new Http2ConnectionManager();

            System.out.println("Server started " + (enableSSL ? "with SSL" : "without SSL"));

            while (true) {
                Socket clientSocket = acceptor.acceptConnection();

                // Check if client is connected
                if (!connectionManager.isClientConnected(clientSocket)) {
                    int threadId = threadCounter.incrementAndGet();
                    System.out.println(
                            "Creating thread #" + threadId + " - Total active threads: " + threadCounter.get());

                    new Thread(() -> {
                        try {
                            System.out.println("Thread #" + threadId + " started");
                            clientSocket.setSoTimeout(30000);

                            // Determine if this is an HTTP/2 connection
                            boolean isHttp2 = false;
                            if (clientSocket.getProperty("protocol") != null
                                    && clientSocket.getProperty("protocol").equals("h2")) {
                                isHttp2 = true;
                            }

                            // Chooose handler based on HTTP protocol version
                            if (isHttp2) {
                                handleHttp2Connection(clientSocket, http2ConnectionManager, threadId);
                            } else {
                                handleHttp1Connection(clientSocket, connectionManager, threadId);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                System.out.println("Thread #" + threadId + " terminated");
                                threadCounter.decrementAndGet();
                                System.out.println("Remaining active threads: " + threadCounter.get());
                                connectionManager.removeClient(clientSocket);
                                clientSocket.close();
                            } catch (IOException closeEx) {
                                closeEx.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    System.out.println("Duplicate connection detected, closing: "
                            + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleHttp1Connection(Socket clientSocket, ConnectionManager connectionManager, int threadId)
            throws IOException {
        Processor processor = new Processor();
        Responder responder = new Responder();

        PushbackInputStream pbInputStream = new PushbackInputStream(clientSocket.getInputStream(), 1);
        OutputStream outputStream = clientSocket.getOutputStream();

        int requestCount = 0;

        while (!clientSocket.isClosed()) {
            try {
                int peekedByte = pbInputStream.read();
                if (peekedByte != -1) {
                    requestCount++;

                    pbInputStream.unread(peekedByte);

                    System.err.println("Thread #" + threadId + " processing HTTP/1.1 request #" + requestCount);
                    HttpRequest request = processor.parseRequest(pbInputStream);
                    HttpResponse response = processor.processRequest(request);
                    responder.sendResponse(response, outputStream);

                    boolean keepAlive = request.getRequestHeaders().containsKey("Connection")
                            && request.getRequestHeaders().get("Connection").equals("keep-alive");

                    if (!keepAlive) {
                        break;
                    }

                } else {
                    System.out.println("Clinet closed the connection");
                    break;
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Connection to " + clientSocket.getInetAddress().getHostName() + " timed out");
                break;
            } catch (IOException e) {
                System.err.println("IO error processing request: " + e.getMessage());
                break;
            }
        }
        connectionManager.removeClient(clientSocket);
    }

    private static void handleHttp2Connection(Socket clientSocket, Http2ConnectionManager connectionManager,
            int threadId) throws IOException {
        connectionManager.addConnectedClient(clientSocket);

        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();

        Http2Processor processor = new Http2Processor(connectionManager);
        Http2Responder responder = new Http2Responder(connectionManager);

        processor.initialise(outputStream);

        // Process HTTP/2 frames
        try {
            while (!clientSocket.isClosed()) {
                try {
                    HttpResponse response = processor.processNextFrame(inputStream);

                    // If there is a response to send, send it
                    if (response != null) {
                        Http2Stream stream = connectionManager.getStream(1);
                        responder.sendResponse(response, stream, outputStream);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println(
                            "HTTP/2 connection to " + clientSocket.getInetAddress().getHostName() + " timed out");
                    break;
                } catch (IOException e) {
                    System.err.println("IO error processing HTTP/2 frame: " + e.getMessage());
                    break;
                }
            }
        } finally {
            connectionManager.removeClient(clientSocket);
        }
    }
}
