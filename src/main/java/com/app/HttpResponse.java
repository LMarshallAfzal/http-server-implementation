package com.app;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * The HttpResponse class represents an HTTP response message.
 * It encapsulates all the components of an HTTP response including status code,
 * protocol version, headers, and body content.
 * 
 * <p>This class is used to construct the response that will be sent back to the client
 * after processing their HTTP request.<p>
 */
public class HttpResponse {
    private String statusCode;
    private final String protocolVersion;
    private HashMap<String, String> headers;
    private String body;
    private byte[] compressedBody;

    /**
     * Constructs a complete HTTP response with all components.
     * 
     * @param protocolVersion the HTTP protocol version (e.g., "HTTP/1.1")
     * @param body the response body content
     */
    public HttpResponse(String protocolVersion, String body) {
        this.statusCode = "200 OK";
        this.protocolVersion = protocolVersion;
        this.body = body;

        Instant nowUtc = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String date = formatter.format(nowUtc);

        this.headers = new HashMap<>();
        this.headers.put("Date", date);
        this.headers.put("Content-Type", "text/plain");
        this.headers.put("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
    }

    /**
     * Constructs HTTP response without a body.
     * 
     * @param protocolVersion the HTTP protocol version (e.g., "HTTP/1.1")
     */
    public HttpResponse(String protocolVersion) {
        this.statusCode = "200 OK";
        this.protocolVersion = protocolVersion;

        Instant nowUtc = Instant.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String date = formatter.format(nowUtc);

        this.headers = new HashMap<>();
        this.headers.put("Date", date);
        this.headers.put("Content-Type", "text/plain");
    }

    /**
     * Gets the HTTP status code.
     * 
     * @return the status code string
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code.
     * 
     * @param statusCode the status code to set
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the protocol version.
     * 
     * @return the protocol version string
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Gets the HTTP headers.
     * 
     * @return a map of header name-value pairs
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets a new header to headers hashmap
     *
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        if (headers.containsKey(name) && name.equals("Content-Length")) {
            this.headers.remove(name);
            this.headers.put(name, value);
        } else {
            this.headers.put(name, value);
        }
    }

    /**
     * Sets the HTTP headers.
     * 
     * @param headers a map of header name-value pairs
     */
    public void setHeaders(HashMap<String, String> headers) {
        this.headers.putAll(headers);
    }

    /**
     * Gets the response body content.
     * 
     * @return the body string
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the response body content.
     * 
     * @param body the body content to set
     */
    public void setBody(String body) {
        this.body = body;
        this.setHeader("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
    }

    /**
     * Gets the response body content.
     *
     * @return the body string
     */
    public byte[] getCompressedBody() {
        return compressedBody;
    }

    /**
     * Sets the compressed response body content.
     *
     * @param compressedBody the compressed body content to set
     */
    public void setCompressedBody(byte[] compressedBody) {
        this.compressedBody = compressedBody;
        this.setHeader("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
    }

    /**
     * Checks if response body has been compressed
     *
     * @return a boolean: true if is compressed, false if not
     */
    public boolean isCompressed() {
        return compressedBody != null;
    }
}