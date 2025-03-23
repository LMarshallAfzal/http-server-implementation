package com.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class Http2Processor {

    private final Http2ConnectionManager connectionManager;

    public Http2Processor(Http2ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Process a single HTTP/2 frame from the input stream
     * 
     * @param inputStream The socket input stream
     * @return An HTTP response if this frame completes a request, null otherwise
     */
    public HttpResponse processNextFrame(InputStream inputStream) throws IOException {
        // Read single frame
        ByteBuffer frameHeaderBuffer = ByteBuffer.allocate(9);
        readFully(inputStream, frameHeaderBuffer);
        frameHeaderBuffer.flip();

        // Pare frame header
        int length = (frameHeaderBuffer.get() & 0xFF) << 16 |
                (frameHeaderBuffer.get() & 0xFF) << 8 |
                (frameHeaderBuffer.get() & 0xFF);
        int type = frameHeaderBuffer.get() & 0xFF;
        int flags = frameHeaderBuffer.get() & 0xFF;
        int streamId = (frameHeaderBuffer.get() & 0x7F) << 24 |
                (frameHeaderBuffer.get() & 0xFF) << 16 |
                (frameHeaderBuffer.get() & 0xFF) << 8 |
                (frameHeaderBuffer.get() & 0xFF);

        // Read payload
        ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
        readFully(inputStream, payloadBuffer);
        payloadBuffer.flip();

        // Process frame based on type
        return processFrame(type, flags, streamId, payloadBuffer);
    }

    /**
     * Initialise HTTP/2 connection by sending initial settings
     *
     * @param outputStream The socket output stream
     */
    public void initialise(OutputStream outputStream) throws IOException {
        // Send initial SETTINGS frame
        SettingsFrame settingsFrame = new SettingsFrame(connectionManager.getServerSettings());
        connectionManager.sendFrame(settingsFrame, outputStream);
    }

    private HttpResponse processFrame(int type, int flags, int streamId, ByteBuffer payload) throws IOException {
        HttpResponse response = null;

        switch (type) {
            case Http2Frame.TYPE_DATA:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_HEADERS:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_PRIORITY:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_RST_STREAM:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_SETTINGS:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_PING:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_GOAWAY:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_WINDOW_UPDATE:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_CONTINUATION:
                response = processDataFrame(streamId, flags, payload);
                break;
            default:
                // Unknown frame type, ignore per spec
                System.out.println("Ignore unknown frame type: " + type);
                break;
        }
        return response;
    }

    private HttpResponse processDataFrame(int streamId, int flags, ByteBuffer payload) throws IOException {

        if (streamId == 0) {
            sendGoAway(Http2Frame.PROTOCOL_ERROR);
            return null;
        }

        Http2Stream stream = connectionManager.getStream(streamId);
        if (stream == null) {
            // Stream doesn't exist, send RST_STREAM
            sendRstStream(streamId, Http2Frame.STREAM_CLOSED);
        }

        boolean endStream = (flags & Http2Stream.FLAG_END_STREAM) != 0;

        // Check flow control
        if (!connectionManager.consumeConnectionWindow(payload.remaining()) ||
                !stream.consumeLocalWindow(payload.remaining())) {
            // Flow control error
            sendRstStream(streamId, Http2Stream.FLOW_CONTROL_ERROR);
            return null;
        }

        // Process the data
        stream.receiveData(payload, endStream);

        // If this completes a request, process it and return the response
        if (endStream && stream.isHeadersReceived()) {
            return createResponse(stream);
        }

        return null;
    }

    private HttpResponse processHeadersFrame(int streamId, int flags, ByteBuffer payload) throws IOException {
        // Implement this method

        if (endStream) {
            return createResponse(stream);
        }

        return null;
    }

    // TODO: Create other frame processing methods

    private HttpResponse createResponse(Http2Stream stream) {
        // Convert HTTP/2 stream to HTTP request
        Map<String, String> headers = stream.getRequestHeaders();
        ByteBuffer data = stream.getData();

        // Extract method, path, etc. from headers
        String method = headers.get(":method");
        String path = headers.get(":path");
        String scheme = headers.get(":scheme");
        String authority = headers.get(":authority");

        if (method == null || path == null || scheme == null) {
            try {
                sendRstStream(stream.getStreamId(), Http2Stream.PROTOCOL_ERROR);
            } catch (IOException e) {
                // TODO: HANDLE ERROR
            }
            return null;
        }

        System.out.println("HTTP/2 Request: " + method + " " + path);

        // Create an HttpRequest object
        HttpRequest request = new HttpRequest();
        request.setMethod(method);
        request.setPath(path);

        // Add headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(":")) {
                request.addHeader(name, entry.getValue());
            }
        }

        // Add request body if present
        if (data.hasRemaining()) {
            byte[] body = new byte[data.remaining()];
            data.get(body);
            request.setBody(body);
        }

        // Process the request to get a response
        // NOTE: Delegate to standard processing class
        HttpResponse response = processRequest(request);

        stream.setResponse(response);

        return response;

    }

    public HttpRequest processRequest(HttpRequest request) {
        // TODO: Use existing request processing logic

        HttpResponse response = new HttpResponse(request.getProtocolVersion());
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.addHeader("Content-type", "text/plain");
        response.setBody("Hello from Http/2!".getBytes());
        return response;
    }

    // Utility methods
    private void readFully(InputStream inputStream, ByteBuffer buffer) throws IOException {
        byte[] bytes = new byte[buffer.remaining()];
        int offset = 0;
        int length = bytes.length;

        while (length > 0) {
            int read = inputStream.read(bytes, offset, length);

            if (read == -1) {
                throw new IOException("Connection closed unexpectedly");
            }

            offset += read;
            length -= read;
        }

        buffer.put(bytes);
    }
}
