package com.app;

import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client socket connections to the HTTP server.
 *
 * <p>
 * This class provides functionality to track, add, and remove client
 * connections
 * using a thread-safe concurrent hash map. Each connection is uniquely
 * identified by
 * a combination of the client's IP address and port number.
 * </p>
 */
public class ConnectionManager {
    private final ConcurrentHashMap<String, Socket> connectedClients = new ConcurrentHashMap<>();

    /**
     * Generates a unique connection ID for a socket.
     *
     * @param socket The client socket for which to generate an ID
     * @return A string in the format "ipv6Address:port" that uniquely identifies
     *         the connection
     */
    private String getConnectionId(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    /**
     * Checks if a client is already connected to the server.
     *
     * @param socket The client socket to check
     * @return true if the client is already connected
     */
    public boolean isClientConnected(Socket socket) {
        String connectionId = getConnectionId(socket);
        return connectedClients.containsKey(connectionId);
    }

    /**
     * Adds a new client connection to the manager.
     *
     * @param socket The client socket to add
     */
    public void addConnectedClient(Socket socket) {
        String connectionId = getConnectionId(socket);
        connectedClients.put(connectionId, socket);
        System.out.println("Added client: " + connectionId);
    }

    /**
     * Removes a client connection from the manager
     *
     * @param socket The client socket to remove
     */
    public void removeClient(Socket socket) {
        String connectionId = getConnectionId(socket);
        connectedClients.remove(connectionId);
        System.out.println("Removed client: " + connectionId);
    }

    /**
     * Returns all currently connected client sockets.
     *
     * @return A collection of all client sockets currently managed by this class
     */
    public Collection<Socket> getAllConnectedClients() {
        return connectedClients.values();
    }
}
