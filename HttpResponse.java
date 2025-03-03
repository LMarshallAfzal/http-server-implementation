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
    private String protocolVersion;
    private String urlPath;
    private String message;
    private HashMap<String, String> headers;
    private String body;

    /**
     * Constructs a complete HTTP response with all components.
     * 
     * @param statusCode the HTTP status code and reason phrase (e.g., "200 OK")
     * @param protocolVersion the HTTP protocol version (e.g., "HTTP/1.1")
     * @param urlPath the requested URL path
     * @param headers a map of HTTP headers (name-value pairs)
     * @param body body the respose body content
     */
    public HttpResponse(String statusCode, String protocolVersion, String urlPath, HashMap<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.protocolVersion = protocolVersion;
        this.urlPath = urlPath;
        this.body = body;
        this.headers = headers;
    }

    /**
     * Constructs HTTP response without a body.
     * 
     * @param statusCode the HTTP status code and reason phrase (e.g., "200 OK")
     * @param protocolVersion the HTTP protocol version (e.g., "HTTP/1.1")
     * @param urlPath the requested URL path
     * @param headers a map of HTTP headers (name-value pairs)
     */
    public HttpResponse(String statusCode, String protocolVersion, String urlPath, HashMap<String, String> headers) {
        this.statusCode = statusCode;
        this.protocolVersion = protocolVersion;
        this.urlPath = urlPath;
        this.headers = headers;
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
     * Sets the HTTP protocol version.
     * 
     * @param protocolVersion the protocol version to set.
     */
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * Gets the requested URL path.
     * 
     * @return  the URL path string
     */
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * Sets the requested URL path.
     * 
     * @param urlPath the URL path to set
     */
    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    /**
     * Gets the response message.
     * 
     * @return the message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the response message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
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
     * Sets the HTTP headers.
     * 
     * @param headers a map of header name-value pairs
     */
    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
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
    }
}