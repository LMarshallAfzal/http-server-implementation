package com.app;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final ConcurrentHashMap<String, Socket> connectedClients = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Socket> getAllConnectedClients() {
        return connectedClients;
    }

    public void addConnectedClient(String hostname, Socket client) {
        connectedClients.put(hostname, client);
    }

    public boolean isClientConnected(String hostname) {
        return connectedClients.containsKey(hostname);
    }

    public void removeClient(String hostname) {
        connectedClients.remove(hostname);
    }
}
