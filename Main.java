import java.io.IOException;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        Acceptor acceptor = new Acceptor();

        while(true) {
            Socket clientSocket = acceptor.acceptConnections();

            new Thread(() -> {
                try {
                    Processor processor = new Processor();
                    HttpResponse response = processor.processRequest(processor.parseRequest(clientSocket.getInputStream()));
                    
                    Responder responder = new Responder();
                    responder.sendResponse(response, clientSocket.getOutputStream());

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
    
}
