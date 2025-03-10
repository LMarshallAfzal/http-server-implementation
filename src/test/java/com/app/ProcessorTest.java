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
        HttpRequest request = processor.parseRequest(inputStream);

        assertEquals("GET", request.getMethod());
        assertEquals("HTTP/1.1", request.getProtocolVersion());
        assertEquals("/test", request.getUrlPath());
        assertEquals("localhost:8080", request.getRequestHeaders().get("Host"));
        assertEquals("Mozilla/5.0", request.getRequestHeaders().get("User-Agent"));
        assertEquals("text/html", request.getRequestHeaders().get("Accept"));
    }

    @Test
    void testParseRequest_ValidPostRequest() throws IOException {
        String httpRequest = "POST /api/data HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 0\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = processor.parseRequest(inputStream);

        assertEquals("POST", request.getMethod());
        assertEquals("HTTP/1.1", request.getProtocolVersion());
        assertEquals("/api/data", request.getUrlPath());
        assertEquals("localhost:8080", request.getRequestHeaders().get("Host"));
        assertEquals("application/json", request.getRequestHeaders().get("Content-Type"));
        assertEquals("0", request.getRequestHeaders().get("Content-Length"));
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
        HttpRequest request = processor.parseRequest(inputStream);

        assertEquals("GET", request.getMethod());
        assertEquals("HTTP/1.1", request.getProtocolVersion());
        assertEquals("/test", request.getUrlPath());
    }

    @Test
    void testParseRequest_HeaderWithNoValue() throws IOException {
        String httpRequest = "GET / HTTP/1.1\r\n" +
                "EmptyHeader:\r\n\r\n";

        InputStream inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = processor.parseRequest(inputStream);

        assertEquals("GET", request.getMethod());
        assertEquals("HTTP/1.1", request.getProtocolVersion());
        assertEquals("/", request.getUrlPath());
        assertEquals("", request.getRequestHeaders().get("EmptyHeader"));
    }

    @Test
    void testProcessRequest_RootEndpoint_Get() {
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/", new HashMap<>());

        HttpResponse response = processor.processRequest(request);

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
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", endpoint, new HashMap<>());

        HttpResponse response = processor.processRequest(request);

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
        HttpRequest request = new HttpRequest("POST", "HTTP/1.1", endpoint, new HashMap<>());

        HttpResponse response = processor.processRequest(request);

        assertEquals("405 Method Not Allowed", response.getStatusCode());
        assertEquals("Method not supported", response.getBody());
    }

    @Test
    void testProcessRequest_UnsupportedEndpoint() {
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/unknown", new HashMap<>());

        HttpResponse response = processor.processRequest(request);

        assertEquals("404 NOT FOUND", response.getStatusCode());
        assertEquals("Cannot find what you are looking for.", response.getBody());
    }

    @Test
    void testProcessRequest_UnsupportedMethod() {
        HttpRequest request = new HttpRequest("DELETE", "HTTP/1.1", "/", new HashMap<>());

        HttpResponse response = processor.processRequest(request);

        assertEquals("405 Method Not Allowed", response.getStatusCode());
        assertEquals("Method not supported", response.getBody());
    }

    @Test
    void testExecuteCommand_SuccessfulExecution() throws Exception {
        HttpResponse response = new HttpResponse("HTTP/1.1");

        Method executeCommandMethod = Processor.class.getDeclaredMethod("executeCommand", ProcessBuilder.class, HttpResponse.class);
        executeCommandMethod.setAccessible(true);

        ProcessBuilder echoCommand = new ProcessBuilder("echo", "test");

        executeCommandMethod.invoke(processor, echoCommand, response);

        assertEquals("200 OK", response.getStatusCode());
        assertTrue(response.getBody().contains("test"), "Response body should contain the echo output");
    }

    @Test
    void testExecuteCommand_FailedExecution() throws Exception {
        // Create a test HttpResponse
        HttpResponse response = new HttpResponse("HTTP/1.1");

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