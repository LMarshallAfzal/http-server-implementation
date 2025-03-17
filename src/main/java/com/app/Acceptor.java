package com.app;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.net.ssl.*;

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
    private final ServerSocket serverSocket;
    private Socket clientSocket;
    private final boolean isSecure;
    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 8443;

    /**
     * Constructs an Acceptor that listens on port 8443 (SSL/TLS) or port 8080
     *
     * @throws IOException IOException if the server socket cannot be created or bound to port 8080.
     */
    public Acceptor(boolean enableSSL) throws IOException {
        isSecure = enableSSL;

        if (enableSSL) {
            File keystore = new File("keystore.jks");
            if (!keystore.exists()) {
                throw new IOException("Keystore file 'keystore.jks' not found. Please create it using keytool.");
            }

            try {
                System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
                System.setProperty("javax.net.ssl.keyStorePassword", "password");
                
                SSLContext sslContext = SSLContext.getInstance("TLS");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream("keystore.jks"), "password".toCharArray());
                kmf.init(ks, "password".toCharArray());
                sslContext.init(kmf.getKeyManagers(), null, null);

                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

                SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(HTTPS_PORT);

                sslServerSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});

                SSLParameters sslParameters = sslServerSocket.getSSLParameters();
                
                sslParameters.setApplicationProtocols(new String[] {"h2", "http/1/1"});

                sslServerSocket.setSSLParameters(sslParameters);

                serverSocket = sslServerSocket;
                System.out.println("Secure server with ALPN support listening on port " + HTTPS_PORT);
                System.out.println("Supported protocols: " + Arrays.toString(sslParameters.getApplicationProtocols()));

            } catch (Exception e) {
                System.err.println("Failed server listening on port " + HTTPS_PORT);
                e.printStackTrace();
                throw new IOException("SSL initialization failed", e);
            }
        } else {
            serverSocket = new ServerSocket(HTTP_PORT);
            System.out.println("Server listening on port " + HTTP_PORT);
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
    public Socket acceptConnection() throws IOException {
        Socket socket = serverSocket.accept();
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.startHandshake();
            String protocol = sslSocket.getApplicationProtocol();

            if ("h2".equals(protocol)) {
                System.out.println("HTTP/2 connection established");
                boolean prefaceIsValid = verifyHttp2ConnectionPreface(sslSocket);
                if (!prefaceIsValid) {
                    sslSocket.close();
                    return acceptConnection();
                }
                socket.setPropert("protocol", "h2");
            } else {
                System.out.println("HTTP/1.1 connection established");
            }
        }
        return socket;
    }

    private boolean verifyHttp2ConnectionPreface(Socket socket) throws IOException {
        static byte[] CONNECTION_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        byte[] preface = new byte[CONNECTION_PREFACE.length];
        
        InputStream inputStream = new InputStream();
        int bytesRead = inputStream.read(preface, 0, preface.length);

        if (bytesRead != CONNECTION_PREFACE.length) {
            System.err.println("Invalid HTTP/2 preface: incomplete read");
            return false;
        }

        if (!Arrays.equals(preface, CONNECTION_PREFACE)) {
            System.err.println("Invalid HTTP/2 preface: incorrect magic string");
            return false;
        }

        System.out.println("Valid HTTP/2 connection preface received");
        return true;
    }

    /**
     * Closes the server socket and releases any associated resources.
     * 
     * @throws IOException if an I/O error occurs when closing the socket
     */
    public void close() throws IOException {
        if (serverSocket != null) {
            System.out.println("Server socket closed!");
            serverSocket.close();
        }
    }
}
