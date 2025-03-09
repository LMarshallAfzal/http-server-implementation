package com.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ProcessorTest {
    private Processor processor;

    @BeforeEach
    public void setUp() {
        processor = new Processor();
    }

    @Test
    void testParseRequest_ValidGetRequest() throws IOException {
        String httpRequest = "GET /test HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "User-Agent: Mozilla/5.0\r\n" +
                "Accept: text/html\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpResponse response = processor.parseRequest(inputStream);

        assertEquals("GET", response.getMethod());
        assertEquals("HTTP/1.1", response.getProtocolVersion());
        assertEquals("/test", response.getUrlPath());
        assertEquals("localhost:8080", response.getHeaders().get("Host"));
        assertEquals("Mozilla/5.0", response.getHeaders().get("User-Agent"));
        assertEquals("text/html", response.getHeaders().get("Accept"));
    }

    @Test
    void testParseRequest_ValidPostRequest() throws IOException {
        String httpRequest = "POST /api/data HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 0\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpResponse response = processor.parseRequest(inputStream);

        assertEquals("POST", response.getMethod());
        assertEquals("HTTP/1.1", response.getProtocolVersion());
        assertEquals("/api/data", response.getUrlPath());
        assertEquals("localhost:8080", response.getHeaders().get("Host"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("0", response.getHeaders().get("Content-Length"));
    }


    @Test
    void testParseRequest_EmptyRequest_ThrowsException() {
        String httpRequest = "";
        
        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        
        assertThrows(IOException.class, () -> {
            processor.parseRequest(inputStream);
        }, "Empty request should throw an NullPointerException.");
    }

    @Test
    void testParseRequest_RequestWithNoProtocolVersion() {
        String httpRequest = "GET /test\r\n" +
                "Host: localhost:8080\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> {
            processor.parseRequest(inputStream);
        }, "Request with no protocol version should throw exception");
    }

    @Test
    void testParseRequest_ExtraSpacesInRequestLine() throws IOException {
        String httpRequest = "GET   /test   HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpResponse response = processor.parseRequest(inputStream);

        assertEquals("GET", response.getMethod());
        assertEquals("HTTP/1.1", response.getProtocolVersion());
        assertEquals("/test", response.getUrlPath());
    }

    @Test
    void testParseRequest_HeaderWithNoValue() throws IOException {
        String httpRequest = "GET / HTTP/1.1\r\n" +
                "EmptyHeader:\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpResponse response = processor.parseRequest(inputStream);

        assertEquals("GET", response.getMethod());
        assertEquals("HTTP/1.1", response.getProtocolVersion());
        assertEquals("/", response.getUrlPath());
        assertEquals("", response.getHeaders().get("EmptyHeader"));
    }

    @Test
    void testProcessRequest_RootEndpoint_Get() {
        HttpResponse response = new HttpResponse("GET", "HTTP/1.1", "/", new HashMap<>());

        response = processor.processRequest(response);

        assertEquals("200 OK", response.getStatusCode());
        assertEquals("Successful GET Request", response.getBody());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/system/info", "/system/memory", "/system/disk",
            "/network/iface", "/network/ip", "/network/ping",
            "/hardware/cpu", "/hardware/load", "/hardware/processes",
            "/util/time", "/util/logs", "/health"
    })
    void testProcessRequest_SupportedEndpoints_Get(String endpoint) {
        HttpResponse response = new HttpResponse("GET", "HTTP/1.1", endpoint, new HashMap<>());

        response = processor.processRequest(response);

        // Cannot assert exact values since command execution depends on system
        assertNotNull(response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/system/info", "/system/memory", "/system/disk",
            "/network/iface", "/network/ip", "/network/ping",
            "/hardware/cpu", "/hardware/load", "/hardware/processes",
            "/util/time", "/util/logs", "/health"
    })
    void testProcessRequest_SupportedEndpoints_Post(String endpoint) {
        HttpResponse response = new HttpResponse("POST", "HTTP/1.1", endpoint, new HashMap<>());

        response = processor.processRequest(response);

        assertEquals("405 Method Not Allowed", response.getStatusCode());
        assertEquals("Method not supported", response.getBody());
    }

    @Test
    void testProcessRequest_UnsupportedEndpoint() {
        HttpResponse response = new HttpResponse("GET", "HTTP/1.1", "/unknown", new HashMap<>());

        response = processor.processRequest(response);

        assertEquals("404 NOT FOUND", response.getStatusCode());
        assertEquals("Cannot find what you are looking for.", response.getBody());
    }

    @Test
    void testProcessRequest_UnsupportedMethod() {
        HttpResponse response = new HttpResponse("DELETE", "HTTP/1.1", "/", new HashMap<>());

        response = processor.processRequest(response);

        assertEquals("405 Method Not Allowed", response.getStatusCode());
        assertEquals("Method not supported", response.getBody());
    }

    @Test
    void testExecuteCommand_SuccessfulExecution() throws Exception {
        // Create a test HttpResponse
        HttpResponse response = new HttpResponse("GET", "HTTP/1.1", "/test", new HashMap<>());

        // Use Java reflection to access the private executeCommand method
        Method executeCommandMethod = Processor.class.getDeclaredMethod("executeCommand", ProcessBuilder.class, HttpResponse.class);
        executeCommandMethod.setAccessible(true);

        // Command that should succeed on most systems
        ProcessBuilder echoCommand = new ProcessBuilder("echo", "test");

        // Execute the command through reflection
        executeCommandMethod.invoke(processor, echoCommand, response);

        assertEquals("200 OK", response.getStatusCode());
        assertTrue(response.getBody().contains("test"), "Response body should contain the echo output");
    }

    @Test
    void testExecuteCommand_FailedExecution() throws Exception {
        // Create a test HttpResponse
        HttpResponse response = new HttpResponse("GET", "HTTP/1.1", "/test", new HashMap<>());

        // Use Java reflection to access the private executeCommand method
        Method executeCommandMethod = Processor.class.getDeclaredMethod("executeCommand", ProcessBuilder.class, HttpResponse.class);
        executeCommandMethod.setAccessible(true);

        // Command that should fail (non-existent command)
        ProcessBuilder invalidCommand = new ProcessBuilder("nonexistentcommand");

        // Execute the command through reflection
        executeCommandMethod.invoke(processor, invalidCommand, response);

        assertEquals("500 Internal Server Error", response.getStatusCode());
        assertTrue(response.getBody().startsWith("Error executing command") ||
                        response.getBody().startsWith("Command failed"),
                "Response body should indicate command error");
    }
}