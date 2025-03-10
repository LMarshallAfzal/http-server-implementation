package com.app;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

/**
 * The Acceptor class handles incoming network connections for the HTTP server.
 * It manages a server socket that listens on a specified port (8080) and accepts
 * incoming client connection requests.
 * 
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Creating a server socket bound to port 8080</li>
 *   <li>Accepting incoming client connections</li>
 *   <li>Providing access to client sockets</li>
 *   <li>Proper resource cleanup</li>
 * </ul>
 */
public class Acceptor {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean isSecure;

    /**
     * Constructs an Acceptor that listens on port 8443 (SSL/TLS) or port 8080
     *
     * @throws IOException IOException if the server socket cannot be created or bound to port 8080.
     */
    public Acceptor(boolean enableSSL) throws IOException {
        this.isSecure = enableSSL;

        if (enableSSL) {
            File keystore = new File("keystore.jks");
            if (!keystore.exists()) {
                throw new IOException("Keystore file 'keystore.jks' not found. Please create it using keytool.");
            }

            try {
                System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
                System.setProperty("javax.net.ssl.keyStorePassword", "password");

                SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                serverSocket = sslServerSocketFactory.createServerSocket(8443);
                System.out.println("Secure server listening on port 8443");
            } catch (Exception e) {
                System.err.println("Failed server listening on port 8443");
                e.printStackTrace();
                throw new IOException("SSL initialization failed", e);
            }
        } else {
            serverSocket = new ServerSocket(8080);
            System.out.println("Server listening on port 8080");
        }
        clientSocket = null;
    }

    /**
     * Blocks until a client connects to the server, then returns the new client socket.
     * This method updates the internal clientSocket reference.
     * 
     * @return a Socket connected to the client
     * @throws IOException if an I/O error occurs when waiting for a connection
     */
    public Socket acceptConnections() throws IOException {
        clientSocket = serverSocket.accept();
        System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress() + (isSecure ? " (secure connection)" : "") + "\n");
        return clientSocket;
    }

    /**
     * Closes the server socket and releases any associated resources.
     * 
     * @throws IOException if an I/O error occurs when closing the socket
     */
    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
