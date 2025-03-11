package com.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ResponderTest {
    private Responder responder;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setUp() {
        responder = new Responder();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void testSendResponse_BasicResponse() throws IOException {
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setStatusCode("200 OK");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertTrue(output.endsWith("\r\n"));
    }

    @Test
    void testSendResponse_WithBody() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        HttpResponse response = new HttpResponse("HTTP/1.1", "Hello, World!");
        response.setStatusCode("200 OK");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertTrue(output.contains("Content-Length: 13\r\n"));
        assertTrue(output.endsWith("Hello, World!"));
    }

    @Test
    void testSendResponse_WithHeaders() throws IOException {
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setStatusCode("200 OK");
        response.setHeader("Server", "MyServer/1.0");
        response.setHeader("Cache-Control", "no-cache");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Server: MyServer/1.0\r\n"));
        assertTrue(output.contains("Cache-Control: no-cache\r\n"));
    }

    @Test
    void testSendResponse_WithBodyAndHeaders() throws IOException {
        HttpResponse response = new HttpResponse("HTTP/1.1", "Hello, World!");
        response.setStatusCode("200 OK");
        response.setHeader("Server", "MyServer/1.0");
        response.setHeader("Cache-Control", "no-cache");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertTrue(output.contains("Content-Length: 13\r\n"));
        assertTrue(output.contains("Server: MyServer/1.0\r\n"));
        assertTrue(output.contains("Cache-Control: no-cache\r\n"));
        assertTrue(output.endsWith("Hello, World!"));
    }

    @Test
    void testSendResponse_NullBody() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setStatusCode("204 No Content");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("HTTP/1.1 204 No Content\r\n"));
        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertFalse(output.contains("Content-Length:"));
        assertTrue(output.endsWith("\r\n"));
    }

    @Test
    void testSendResponse_EmptyBody() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        HttpResponse response = new HttpResponse("HTTP/1.1", "");
        response.setStatusCode("200 OK");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertTrue(output.contains("Content-Length: 0\r\n"));
    }

    @Test
    void testSendResponse_SpecialCharactersInBody() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        String specialBody = "Hello, 世界! Special chars: €£¥";
        HttpResponse response = new HttpResponse("HTTP/1.1", specialBody);
        response.setStatusCode("200 OK");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Content-Length: " + specialBody.getBytes(StandardCharsets.UTF_8).length + "\r\n"));
        assertTrue(output.endsWith(specialBody));
    }

    @Test
    void testSendResponse_ThrowsIOException() throws IOException {
        // Create a mock OutputStream that throws IOException
        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Simulated IO failure");
            }
        };

        HashMap<String, String> headers = new HashMap<>();
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setStatusCode("200 OK");

        // Assert that IO exception is propagated
        assertThrows(IOException.class, () -> {
            responder.sendResponse(response, failingStream);
        });
    }

    @Test
    void testSendResponse_HeadersOverrideDefaults() throws IOException {
        HttpResponse response = new HttpResponse("HTTP/1.1", "Test body");
        response.setStatusCode("200 OK");
        response.setHeader("Content-Type", "application/json");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
//        assertTrue(output.contains("Content-Type: text/plain\r\n"));
        assertTrue(output.contains("Content-Type: application/json\r\n"));
    }

    @Test
    void testSendResponse_DifferentStatusCodes() throws IOException {
        HashMap<String, String> headers = new HashMap<>();

        HttpResponse response404 = new HttpResponse("HTTP/1.1");
        response404.setStatusCode("404 Not Found");
        responder.sendResponse(response404, outputStream);
        String output404 = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output404.startsWith("HTTP/1.1 404 Not Found\r\n"));

        outputStream.reset();

        HttpResponse response500 = new HttpResponse("HTTP/1.1");
        response500.setStatusCode("500 Internal Server Error");
        responder.sendResponse(response500, outputStream);
        String output500 = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output500.startsWith("HTTP/1.1 500 Internal Server Error\r\n"));
    }

    @Test
    void testSendResponse_LargeBody() throws IOException {
        HashMap<String, String> headers = new HashMap<>();

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 100 * 1024 / 10; i++) {
            largeBody.append("0123456789");
        }

        HttpResponse response = new HttpResponse("HTTP/1.1", largeBody.toString());
        response.setStatusCode("200 OK");

        responder.sendResponse(response, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Content-Length: " + (100 * 1024) + "\r\n"));
        assertEquals(100 * 1024 + output.indexOf(largeBody.toString()), output.length());
    }
}
