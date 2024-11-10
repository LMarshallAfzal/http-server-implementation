// Listens on a specific port for incoming connections
// Accepts incoming connection requests and creates new sockets for each client.
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;


public class Acceptor {
    private ServerSocket serverSocket;

    public Acceptor() throws IOException {
        serverSocket = new ServerSocket(8080);
        System.out.println("Server listening on port 8080");
    }

    public void acceptConnections() throws IOException {
        while (true) {

            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

            new Thread(() -> {
                try {
                    Processor processor = new Processor();
                    processor.parseRequest(clientSocket.getInputStream());
                    clientSocket.close();


                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
