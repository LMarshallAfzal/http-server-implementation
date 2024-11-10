// Listens on a specific port for incoming connections
// Accepts incoming connection requests and creates new sockets for each client.
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class Acceptor {
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public Acceptor() throws IOException {
        serverSocket = new ServerSocket(8080);
        clientSocket = null;
        System.out.println("Server listening on port 8080");
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public Socket acceptConnections() throws IOException {
        clientSocket = serverSocket.accept();
        System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
        return clientSocket;
    }

    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
