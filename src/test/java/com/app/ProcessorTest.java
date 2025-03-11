package com.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

public class ProcessorTest {
    private Processor processor;

    private void invokeCompressResponse(HttpRequest request, HttpResponse response) throws Exception {
        Method method = Processor.class.getDeclaredMethod("compressResponse", HttpRequest.class, HttpResponse.class);
        method.setAccessible(true);
        method.invoke(processor, request, response);
    }

    // Helper method to decompress GZIP data
    private String decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        GZIPInputStream gis = new GZIPInputStream(bis);

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = gis.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        gis.close();
        bis.close();

        return result.toString(StandardCharsets.UTF_8);
    }

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
        HttpResponse response = new HttpResponse("HTTP/1.1");

        Method executeCommandMethod = Processor.class.getDeclaredMethod("executeCommand", ProcessBuilder.class, HttpResponse.class);
        executeCommandMethod.setAccessible(true);

        ProcessBuilder invalidCommand = new ProcessBuilder("nonexistentcommand");

        executeCommandMethod.invoke(processor, invalidCommand, response);

        assertEquals("500 Internal Server Error", response.getStatusCode());
        assertTrue(response.getBody().startsWith("Error executing command") ||
                        response.getBody().startsWith("Command failed"),
                "Response body should indicate command error");
    }

    @Test
    void testCompressResponse_NoAcceptEncodingHeader() throws Exception {
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", new HashMap<>());
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setBody("Test body");
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_AcceptEncodingWithoutGzip() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "deflate, br");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setBody("Test body");
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_NullBody() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setBody(null);

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_EmptyBody() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setBody("");

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_SmallBody() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");
        response.setBody("Small body less than 1024 bytes");
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "image/jpeg", "image/png", "application/octet-stream",
            "audio/mpeg", "video/mp4"
    })
    void testCompressResponse_NonCompressibleContentType(String contentType) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            largeBody.append("a");
        }
        response.setBody(largeBody.toString());
        response.setHeader("Content-Type", contentType);

        invokeCompressResponse(request, response);

        assertNull(response.getCompressedBody());
        assertNull(response.getHeaders().get("Content-Encoding"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "text/html", "text/plain", "text/css",
            "application/json", "application/xml", "application/javascript"
    })
    void testCompressResponse_CompressibleContentType(String contentType) throws Exception {
        // Setup
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "gzip");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeBody.append("a");
        }
        String body = largeBody.toString();
        response.setBody(body);
        response.setHeader("Content-Type", contentType);

        invokeCompressResponse(request, response);

        assertNotNull(response.getCompressedBody());
        assertEquals("gzip", response.getHeaders().get("Content-Encoding"));
        assertNotNull(response.getHeaders().get("Content-Length"));
        assertEquals("Accept-Encoding", response.getHeaders().get("Vary"));

        String decompressed = decompressGzip(response.getCompressedBody());
        assertEquals(body, decompressed);
    }

    @Test
    void testCompressResponse_MultipleEncodings() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "deflate, gzip, br");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeBody.append("a");
        }
        response.setBody(largeBody.toString());
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNotNull(response.getCompressedBody());
        assertEquals("gzip", response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_WithQualityValue() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "deflate;q=0.9, gzip;q=0.8, *;q=0.1");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeBody.append("a");
        }
        response.setBody(largeBody.toString());
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNotNull(response.getCompressedBody());
        assertEquals("gzip", response.getHeaders().get("Content-Encoding"));
    }

    @Test
    void testCompressResponse_GzipWithUpperCase() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "GZIP");
        HttpRequest request = new HttpRequest("GET", "HTTP/1.1", "/test", headers);
        HttpResponse response = new HttpResponse("HTTP/1.1");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeBody.append("a");
        }
        response.setBody(largeBody.toString());
        response.setHeader("Content-Type", "text/html");

        invokeCompressResponse(request, response);

        assertNotNull(response.getCompressedBody());
        assertEquals("gzip", response.getHeaders().get("Content-Encoding"));
    }
}