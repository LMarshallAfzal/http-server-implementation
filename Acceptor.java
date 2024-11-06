// Listens on a specific port for incoming connections
// Accepts incoming connection requests and creates new sockets for each client.
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
                    InputStream input = clientSocket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    String line;

                    while((line = reader.readLine()) != null) {
                        System.out.println(line);

                        if(line.isEmpty()) {
                            break;
                        }
                    }

                    OutputStream outputStream = clientSocket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream, true);
                    writer.println("HTTP/1.1 200 OK");
                    writer.println("Content-Type: text/html");
                    writer.println();
                    writer.flush();

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

    public void handleCommunication() throws IOException {
        
    }

    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
