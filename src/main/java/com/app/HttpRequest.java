package com.app;

import java.util.HashMap;

public class HttpRequest {
    private final String method;
    private final String protocolVersion;
    private final String urlPath;
    private HashMap<String, String> requestHeaders;

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
