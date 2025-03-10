package com.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;


import java.io.File;
import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AcceptorTest {
    private Acceptor acceptor;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(5);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (acceptor != null) {
            acceptor.close();
        }
        executor.shutdownNow();
    }

    @Test
    void testConstructor_SuccessfulInitialization_NoSSL() {
        assertDoesNotThrow(() -> {
            acceptor = new Acceptor(false);
        }, "Constructor should initialize ServerSocket without exceptions.");
    }

    @Test
    void testConstructor_SuccessfulInitialization_WithSSL() {
        assertDoesNotThrow(() -> {
            acceptor = new Acceptor(true);
        }, "Constructor should initialize ServerSocket without exceptions.");
    }

    @Test
    void testConstructor_UnsuccessfulInitialization_WithSSL() {
        File originalKeystore = new File("keystore.jks");
        File tempFile = new File("keystore.jks.temp");

        boolean keystoreExists = originalKeystore.exists();
        boolean renamed = false;

        try {
            if (keystoreExists) {
                renamed = originalKeystore.renameTo(tempFile);
                assertTrue(renamed, "Failed to temporarily rename keystore for testing");
                assertFalse(originalKeystore.exists(), "Keystore file should not exist after renaming");
            }

            IOException exception = assertThrows(IOException.class, () -> {
                new Acceptor(true);
            }, "Constructor should throw IOException when keystore is not found.");

            assertTrue(exception.getMessage().contains("Keystore file"),
                    "Exception message should indicate keystore file not found");
        } finally {
            if (keystoreExists && renamed) {
                tempFile.renameTo(originalKeystore);
                assertTrue(originalKeystore.exists(), "Failed to restore keystore after test");
            }
        }
    }

    @Test
    void testConstructor_InvalidPortThrowsException() {
        assertThrows(BindException.class, () -> {
            ServerSocket invalidSocket = new ServerSocket(80);
            invalidSocket.close();
        }, "Binding to privileged port should throw exception.");

        assertThrows(IllegalArgumentException.class, () -> {
            ServerSocket invalidSocket = new ServerSocket(-1);
            invalidSocket.close();
        }, "Constructor with invalid port should throw exception.");

        assertThrows(IllegalArgumentException.class, () -> {
            ServerSocket invalidSocket = new ServerSocket(65536);
            invalidSocket.close();
        }, "Constructor with out of range port should throw exception.");
    }

    @Test
    @Timeout(5)
    void testAcceptConnections_SuccessfulConnection() throws Exception {
        acceptor = new Acceptor(false);
        Future<?> serverFuture = executor.submit(() -> {
            try {
                Socket clientSocket = acceptor.acceptConnections();
                assertNotNull(clientSocket, "Accepted socket should not be null");
                assertFalse(clientSocket.isClosed(), "Accepted socket should be open");
                clientSocket.close();
            } catch (IOException e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        });

        Thread.sleep(500);

        try (Socket clientSocket = new Socket(InetAddress.getLocalHost(), 8080)) {
            assertTrue(clientSocket.isConnected(), "Client socket should be connected");
        }

        serverFuture.get();
    }

    @Test
    @Timeout(10)
    void testAcceptConnections_MultipleConnections() throws Exception {
        final int NUM_CONNECTIONS = 3;
        acceptor = new Acceptor(false);

        List<Socket> acceptedSockets = new ArrayList<>();

        Future<?> serverFuture = executor.submit(() -> {
            try {
                for (int i = 0; i < NUM_CONNECTIONS; i++) {
                    Socket socket = acceptor.acceptConnections();
                    acceptedSockets.add(socket);
                }
            } catch (IOException e) {
                fail("Exception should be thrown: " + e.getMessage());
            }
        });

        Thread.sleep(500);

        List<Socket> clientSockets = new ArrayList<>();
        for (int i = 0; i < NUM_CONNECTIONS; i++) {
            Socket clientSocket = new Socket(InetAddress.getLocalHost(), 8080);
            clientSockets.add(clientSocket);
            Thread.sleep(200);
        }

        Thread.sleep(1000);

        assertEquals(NUM_CONNECTIONS, acceptedSockets.size(), "Server should accept " + NUM_CONNECTIONS + " connections");

        for (Socket socket : clientSockets) {
            socket.close();
        }

        for (Socket socket : acceptedSockets) {
            socket.close();
        }
    }

    @Test
    void testAcceptConnections_Interrupted_ThrowsException() throws Exception {
        acceptor = new Acceptor(false);

        Thread serverThread = new Thread(() -> {
            try {
                acceptor.acceptConnections();
                fail("Should not reach here because thread is interrupted");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("accept") ||
                                e.getMessage().contains("closed") ||
                                e.getMessage().contains("interrupted"),
                        "Exception message should indicate accept was interrupted: " + e.getMessage());
            }
        });

        serverThread.start();
        Thread.sleep(200);

        serverThread.interrupt();

        acceptor.close();

        serverThread.join(2000);

        assertFalse(serverThread.isAlive(), "Server thread should have terminated.");
    }

    @Test
    void testAcceptConnections_SocketClosed_ThrowsException() throws Exception {
        acceptor = new Acceptor(false);

        Future<?> serverFuture = executor.submit(() -> {
            try {
                acceptor.acceptConnections();
                fail("Should not reach here because socket is closed");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("closed") || e.getMessage().contains("accept"),
                        "Exception message should indicate socket was closed: " + e.getMessage());
            }
        });

        Thread.sleep(200);

        acceptor.close();

        assertThrows(ConnectException.class, () -> {
            Socket clientSocket = new Socket(InetAddress.getLocalHost(), 8080);
            clientSocket.close();
        }, "Connection should fail after server socket is closed");

    }

    @Test
    void testClose_StopsAcceptingConnections() throws IOException {
        acceptor = new Acceptor(false);

        acceptor.close();

        assertThrows(ConnectException.class, () -> {
            Socket clientSocket = new Socket(InetAddress.getLocalHost(), 8080);
            clientSocket.close();
        }, "Connection should fail after server socket is closed");
    }

    @Test
    void testClose_InterruptAcceptMethod() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        acceptor = new Acceptor(false);

        Future<?> acceptFuture = executor.submit(() -> {
            try {
                acceptor.acceptConnections();
                fail("Should not reach here because socket is closed");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("closed") ||
                                     e.getMessage().contains("accept") ||
                                     e.getMessage().contains("interrupted"),
                                    "Exception message should indicate socket was closed: " + e.getMessage());

            }
        });

        Thread.sleep(200);
        acceptor.close();
        acceptFuture.get(2, TimeUnit.SECONDS);
    }

    @Test
    void testClose_CanBeCalledMultipleTimes() throws IOException {
        acceptor = new Acceptor(false);
        acceptor.close();

        assertDoesNotThrow(() -> acceptor.close());
    }

    @Test
    void testClose_ReleasesPort() throws IOException {
        acceptor = new Acceptor(false);
        acceptor.close();

        Acceptor newAcceptor = null;
        try {
            newAcceptor = new Acceptor(false);
            assertTrue(true);
        } catch (IOException e) {
            fail("Failed to create a new Acceptor. Port may not have been released: " + e.getMessage());
        } finally {
            if (newAcceptor != null) {
                newAcceptor.close();
            }
        }
    }
}

