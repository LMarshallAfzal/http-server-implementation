package com.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Http2Responder {
    private final Http2ConnectionManager connectionManager;

    public Http2Responder(Http2ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void sendResponse(HttpResponse response, Http2Stream stream, OutputStream outputStream) throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(":status", String.valueOf(response.getStatusCode()));

        // Add response headers
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            headers.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        // Send HEADERS frame
        ByteBuffer headerBlock = encodeHeaders(headers);
        HeadersFrame headersFrame = new HeadersFrame(
                stream.getStreamId(),
                headerBlock,
                response.getBody() == null || response.getBody().length() == 0,
                true);

        connectionManager.sendFrame(headersFrame, outputStream);

        // Send DATA frame if there is a body
        if (response.getBody() != null && response.getBody().length() > 0) {
            ByteBuffer data = ByteBuffer.wrap(response.getBody().getBytes());
            DataFrame dataFrame = new DataFrame(stream.getStreamId(), data, true);
            connectionManager.sendFrame(dataFrame, outputStream);
        }
    }

    private ByteBuffer encodeHeaders(HashMap<String, String> headers) {
        // Use HPACK encoder to compress headers
        try {
            ByteArrayOutputStream boas = new ByteArrayOutputStream();

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connectionManager.getEncoder().encodeHeader(
                        boas,
                        entry.getKey().getBytes("UTF-8"),
                        entry.getValue().getBytes("UTF-8"),
                        true);
            }

            return ByteBuffer.wrap(boas.toByteArray());
        } catch (Exception e) {
            System.err.println("Error encoding headers: " + e.getMessage());
            return ByteBuffer.allocate(0);
        }
    }
}
