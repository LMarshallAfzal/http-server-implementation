package com.app;

import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final ConcurrentHashMap<String, Socket> connectedClients = new ConcurrentHashMap<>();

    private String getConnectionId(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public boolean isClientConnected(Socket socket) {
        String connectionId = getConnectionId(socket);
        return connectedClients.containsKey(connectionId);
    }

    public void addConnectedClient(Socket socket) {
        String connectionId = getConnectionId(socket);
        connectedClients.put(connectionId, socket);
        System.out.println("Added client: " + connectionId);
    }

    public void removeClient(Socket socket) {
        String connectionId = getConnectionId(socket);
        connectedClients.remove(connectionId);
        System.out.println("Removed client: " + connectionId);
    }

    public Collection<Socket> getAllConnectedClients() {
        return connectedClients.values();
    }
}
