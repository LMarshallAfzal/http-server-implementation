import java.util.HashMap;

public class HttpResponse {
    private String statusCode;
    private String protocolVersion;
    private String urlPath;
    private String message;
    private HashMap<String, String> headers;
    private String body;

    public HttpResponse(String statusCode, String protocolVersion, String urlPath, HashMap<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.protocolVersion = protocolVersion;
        this.urlPath = urlPath;
        this.body = body;
        this.headers = headers;
    }

    public HttpResponse(String statusCode, String protocolVersion, String urlPath, HashMap<String, String> headers) {
        this.statusCode = statusCode;
        this.protocolVersion = protocolVersion;
        this.urlPath = urlPath;
        this.headers = headers;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}