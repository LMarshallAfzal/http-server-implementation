package com.app;

import java.util.HashMap;

/**
 * The HttpRequest class represents an HTTP request message.
 * It encapsulates all the components of an HTTP request including status code,
 * protocol version, headers, and body content.
 *
 * <p>
 * This class is used to construct the request that will be parsed and
 * processed.
 * </p>
 */
public class HttpRequest {
    private final String method;
    private final String protocolVersion;
    private final String urlPath;
    private HashMap<String, String> requestHeaders;

    /**
     * Constructs a complete HTTP request with all components.
     *
     * @param method          the HTTP request method (e.g., "GET")
     * @param protocolVersion the HTTP protocol version (e.g., "HTTP/1.1")
     * @param urlPath         the URL path to send to (e.g., "/system/info")
     * @param requestHeaders  the HTTP request headers hashmap (e.g., (Connection:
     *                        keep-alive))
     */
    public HttpRequest(String method, String protocolVersion, String urlPath, HashMap<String, String> requestHeaders) {
        this.method = method;
        this.protocolVersion = protocolVersion;
        this.urlPath = urlPath;
        this.requestHeaders = requestHeaders;
    }

    /**
     * Gets the HTTP method.
     *
     * @return the method string
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the requested URL path.
     *
     * @return the URL path string
     */
    public String getUrlPath() {
        return urlPath;
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
     * Gets the HTTP request headers.
     *
     * @return a map of header name-value pairs
     */
    public HashMap<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Sets the HTTP request headers.
     *
     * @param requestHeaders a map of header name-value pairs
     */
    public void setRequestHeaders(HashMap<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
}
