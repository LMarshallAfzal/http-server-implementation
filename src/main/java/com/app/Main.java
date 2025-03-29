package com.app;

import java.io.IOException;

/**
 * The Main class is the entry point for the HTTP server application.
 * It createst a Server instance to handle both HTTP/1.1 HTTP/2 connections
 */
public class Main {
    /**
     * The main method that starts the HTTP server.
     * 
     * @param args command line arguments '--ssl' for encryption
     * @throws IOException if an I/O error occurs when creating the Acceptor
     */
    public static void main(String[] args) throws IOException {
        boolean enableSSL = args.length > 0 && args[0].equalsIgnoreCase("--ssl");

        try {
            // Create server with specified SSL setting and a thread pool size of 10
            Server server = new Server(enableSSL, 10);

            // Add shutdown hook to gracefully terminate the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop();
            }));

            // Start the server
            server.start();

            System.out.println("Server started " + (enableSSL ? "with SSL" : "without SSL"));
            System.out.println("Press Ctrl+C to stop the server");

        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
