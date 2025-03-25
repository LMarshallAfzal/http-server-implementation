package com.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Server class is the main coordinator of the HTTP server
 * It manages both HTTP/1.1 and HTTP/2 connections.
 */
public class Server {
    private static AtomicInteger threadCounter = new AtomicInteger();
    private final ExecutorService executorService;
    private final Acceptor acceptor;
    private final ConnectionManager http1ConnectionManager;
    private final Http2ConnectionManager http2ConnectionManager;
    private boolean running;

    /**
     * Creates a new server instance.
     *
     * @param enableSSL      whether to enable SSL/TLS
     * @param threadPoolSize number of threads in the pool
     * @throws IOException if the server socket cannot be created
     */
    public Server(boolean enableSSL, int threadPoolSize) throws IOException {
        this.acceptor = new Acceptor(enableSSL);
        this.http1ConnectionManager = new ConnectionManager();
        this.http2ConnectionManager = new Http2ConnectionManager();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.running = false;
    }

    /**
     * Starts the server, accepting and processing connections
     */
    public void start() {
        running = true;

        System.out.println("Server starting...");

        // Accept connections in a separate thread
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = acceptor.acceptConnection();
                    handleConnection(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        }).start();
        System.out.println("Server started successfully.");
    }

    /**
     * Handles a new client connection.
     *
     * @param clientSocket the clientSocket
     */
    private void handleConnection(Socket clientSocket) {
        // Check if client is already connected
        if (http1ConnectionManager.isClientConnected(clientSocket)) {
            try {
                System.out.println("Duplicate connection detected, closing: "
                        + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing duplicate connection: " + e.getMessage());
            }
            return;
        }

        int threadId = threadCounter.incrementAndGet();
        System.out.println("Handling connection #" + threadId + " - Total active connections: " + threadCounter.get());

        executorService.submit(() -> {
            try {
                System.out.println("Thread #" + threadId + " started for connection from " +
                        clientSocket.getInetAddress().getHostAddress());

                clientSocket.setSoTimeout(30000);

                // Handle the connection with the appropriate protocol handler
                if (acceptor.isHttp2(clientSocket)) {
                    handleHttp2Connection(clientSocket, threadId);
                } else {
                    handleHttp1Connection(clientSocket, threadId);
                }
            } catch (Exception e) {
                System.err.println("Error handling connection #" + threadId + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    System.out.println("Thread #" + threadId + " terminated");
                    threadCounter.decrementAndGet();
                    System.out.println("Remaining active connections: " + threadCounter.get());

                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                } catch (IOException closeEx) {
                    System.err.println("Error closing connection: " + closeEx.getMessage());
                }
            }
        });
    }

    /**
     * Handles an HTTP/1.1 connection.
     * 
     * @param clientSocket the client socket
     * @param threadId     the thread ID for logging
     * @throws IOException if an I/O error occurs
     */
    private void handleHttp1Connection(Socket clientSocket, int threadId) throws IOException {
        http1ConnectionManager.addConnectedClient(clientSocket);

        Processor processor = new Processor();
        Responder responder = new Responder();

        PushbackInputStream pbInputStream = new PushbackInputStream(
                clientSocket.getInputStream(), 1);
        OutputStream outputStream = clientSocket.getOutputStream();

        int requestCount = 0;

        while (!clientSocket.isClosed()) {
            try {
                int peekedByte = pbInputStream.read();
                if (peekedByte != -1) {
                    requestCount++;

                    pbInputStream.unread(peekedByte);

                    System.out.println("Thread #" + threadId + " processing HTTP/1.1 request #" + requestCount);
                    HttpRequest request = processor.parseRequest(pbInputStream);
                    HttpResponse response = processor.processRequest(request);
                    responder.sendResponse(response, outputStream);

                    boolean keepAlive = request.getRequestHeaders().containsKey("Connection")
                            && "keep-alive".equals(request.getRequestHeaders().get("Connection"));

                    if (!keepAlive) {
                        break;
                    }
                } else {
                    System.out.println("Client closed the connection");
                    break;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Connection to " + clientSocket.getInetAddress().getHostName()
                        + " timed out");
                break;
            } catch (IOException e) {
                System.out.println("IO error processing request: " + e.getMessage());
                break;
            }
        }

        http1ConnectionManager.removeClient(clientSocket);
    }

    /**
     * Handles an HTTP/2 connection.
     * 
     * @param clientSocket the client socket
     * @param threadId     the thread ID for logging
     * @throws IOException if an I/O error occurs
     */
    private void handleHttp2Connection(Socket clientSocket, int threadId) throws IOException {
        http2ConnectionManager.addConnectedClient(clientSocket);

        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();

        // Create HTTP/2 processor and responder
        Http2Processor processor = new Http2Processor(http2ConnectionManager);
        Http2Responder responder = new Http2Responder(http2ConnectionManager);

        // Initialize the HTTP/2 connection by sending initial settings
        processor.initialise(outputStream);

        System.out.println("Thread #" + threadId + " established HTTP/2 connection");

        // Process HTTP/2 frames until connection closes
        try {
            while (!clientSocket.isClosed()) {
                try {
                    // Process next frame and get response if available
                    HttpResponse response = processor.processNextFrame(inputStream);

                    // If we have a response to send, send it on the appropriate stream
                    if (response != null && response.getProperty("streamId") != null) {
                        int streamId = (int) response.getProperty("streamId");
                        Http2Stream stream = http2ConnectionManager.getStream(streamId);
                        if (stream != null) {
                            responder.sendResponse(response, stream, outputStream);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("HTTP/2 connection to " + clientSocket.getInetAddress().getHostName()
                            + " timed out");
                    break;
                } catch (IOException e) {
                    System.out.println("IO error processing HTTP/2 frame: " + e.getMessage());
                    break;
                }
            }
        } finally {
            http2ConnectionManager.removeClient(clientSocket);
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        running = false;

        try {
            acceptor.close();
        } catch (IOException e) {
            System.err.println("Error closing acceptor: " + e.getMessage());
        }

        executorService.shutdown();
        System.out.println("Server stopped.");
    }
}
