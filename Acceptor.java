import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

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

    /**
     * Constructs an Acceptor that listens on port 8080.
     * 
     * @throws IOException IOException if the server socket cannot be created or bound to port 8080.
     */
    public Acceptor() throws IOException {
        serverSocket = new ServerSocket(8080);
        clientSocket = null;
        System.out.println("Server listening on port 8080");
    }

    /**
     * Return the current client socket.
     * 
     * @return the current Socket connecton to the client, or null if no client is connected 
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Blocks unil a client connects to the server, then returns the new client socket.
     * This method updates the internal clientSocket reference.
     * 
     * @return a Socket connected to the client
     * @throws IOException if an I/O error occurs when waiting for a connection
     */
    public Socket acceptConnections() throws IOException {
        clientSocket = serverSocket.accept();
        System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");
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
