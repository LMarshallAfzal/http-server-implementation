package com.app;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The Responder class is responsible for sending HTTP responses to clients.
 * It formats the HTTP response according to the HTTP protocol specification
 * and writes it to the client's output stream.
 * 
 * <p>This class handles the final step in the HTTP request-response cycle
 * by sending the formatted response back to the client.</p>
 */
public class Responder {
    
    /**
     * Sends an HTTP response to the client.
     * 
     * <p>This method formats the HTTP response with appropriate status line,
     * headers, and body, then writes it to the provided output stream. It also
     * prints the complete response to the console for debugging purposes.</p>
     * 
     * <p>The response format follows the HTTP specification:</p>
     * <pre>
     * [PROTOCOL_VERSION] [STATUS_CODE]
     * [HEADER_NAME]: [HEADER_VALUE]
     * ...
     * 
     * [BODY]
     * </pre>
     * 
     * @param response the HttpResponse object containing the response information
     * @param outputStream the OutputStream to write the formatted response to
     * @throws IOException if an I/O error occurs while writing to the output stream
     */
    public void sendResponse(HttpResponse response, OutputStream outputStream) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append(response.getProtocolVersion()).append(" ").append(response.getStatusCode()).append("\r\n");

        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            headerBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        headerBuilder.append("\r\n");

        outputStream.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));

        if (response.isCompressed()) {
            outputStream.write(response.getCompressedBody());

        } else if (response.getBody() != null) {
            outputStream.write(response.getBody().getBytes(StandardCharsets.UTF_8));
        }

        outputStream.flush();
    }
}
