package com.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Http2Processor {

    private final Http2ConnectionManager connectionManager;
    private OutputStream outputStream;

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
        this.outputStream = outputStream;

        // Send initial SETTINGS frame
        SettingsFrame settingsFrame = new SettingsFrame(connectionManager.getLocalSettings());
        connectionManager.sendFrame(settingsFrame, outputStream);
    }

    private HttpResponse processFrame(int type, int flags, int streamId, ByteBuffer payload) throws IOException {
        HttpResponse response = null;

        switch (type) {
            case Http2Frame.TYPE_DATA:
                response = processDataFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_HEADERS:
                response = processHeadersFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_PRIORITY:
                processPriorityFrame(streamId, payload);
                break;

            case Http2Frame.TYPE_RST_STREAM:
                // Handle RST_STREAM frame
                int errorCode = payload.getInt(0);
                Http2Stream stream = connectionManager.getStream(streamId);
                if (stream != null) {
                    stream.resetStream(errorCode);
                }
                break;

            case Http2Frame.TYPE_SETTINGS:
                // Handle SETTINGS frame
                processSettingsFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_PING:
                processPingFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_GOAWAY:
                processGoAwayFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_WINDOW_UPDATE:
                processWindowUpdateFrame(streamId, flags, payload);
                break;

            case Http2Frame.TYPE_CONTINUATION:
                processContinuationFrame(streamId, flags, payload);
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

        boolean endStream = (flags & Http2Frame.FLAG_END_STREAM) != 0;

        // Check flow control
        if (!connectionManager.consumeConnectionWindow(payload.remaining()) ||
                !stream.consumeLocalWindow(payload.remaining())) {
            // Flow control error
            sendRstStream(streamId, Http2Frame.FLOW_CONTROL_ERROR);
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
        if (streamId == 0) {
            sendGoAway(Http2Frame.PROTOCOL_ERROR);
            return null;
        }

        Http2Stream stream = connectionManager.getStream(streamId);
        boolean endStream = (flags & Http2Frame.FLAG_END_STREAM) != 0;
        boolean endHeaders = (flags & Http2Frame.FLAG_END_HEADERS) != 0;

        if (stream == null) {
            // Create new stream
            stream = connectionManager.createStream(streamId);
            stream.transitionToOpen();
        }

        // Decode headers using HPACK
        try {
            byte[] headerBlock = new byte[payload.remaining()];
            payload.get(headerBlock);

            ByteArrayInputStream headerInputStream = new ByteArrayInputStream(headerBlock);

            HashMap<String, String> headers = new HashMap<>();
            connectionManager.getDecoder().decode(headerInputStream, (name, value, senstive) -> {
                String nameStr = new String(name, StandardCharsets.UTF_8);
                String valueStr = new String(value, StandardCharsets.UTF_8);
                headers.put(nameStr, valueStr);
            });

            stream.receiveHeaders(headers, endStream);

            // If this completes a request (END_STREAM flag set), process it
            if (endStream) {
                return createResponse(stream);
            }

        } catch (Exception e) {
            sendRstStream(streamId, Http2Frame.INTERNAL_ERROR);
            System.err.println("Error processing HEADERS frame: " + e.getMessage());
        }
        return null;
    }

    private void processPriorityFrame(int streamId, ByteBuffer payload) {
        if (streamId == 0) {
            // Invalid - PRIORITY frames cannot be associated with stream 0
            return;
        }

        if (payload.remaining() < 5) {
            return;
        }

        int dependencyAndE = payload.getInt();
        boolean exclusive = (dependencyAndE & 0x80000000) != 0;
        int streamDependency = dependencyAndE * 0x7FFFFFFF;
        int weight = (payload.get() & 0xFF) + 1;

        Http2Stream stream = connectionManager.getStream(streamId);
        if (stream == null) {
            // Create idle stream for priority information
            stream = connectionManager.createStream(streamId);
        }

        Http2Stream parent = streamDependency != 0 ? connectionManager.getStream(streamDependency) : null;
        stream.setPriority(weight, exclusive, parent);

    }

    private void processSettingsFrame(int streamId, int flags, ByteBuffer payload) throws IOException {
        if (streamId != 0) {
            // SETTINGS frame must be associated with stream 0
            sendGoAway(Http2Frame.PROTOCOL_ERROR);
            return;
        }

        boolean isAck = (flags & Http2Frame.FLAG_ACK) != 0;

        if (isAck) {
            System.out.println("Received SETTINGS ACK");
        }

        // Parse settings
        SettingsFrame settingsFrame = new SettingsFrame(streamId, flags, payload);
        Http2Settings settings = settingsFrame.getSettings();

        connectionManager.updateRemoteSettings(settings);

        // Send ACK
        SettingsFrame ackFrame = new SettingsFrame(true);
        connectionManager.sendFrame(ackFrame, outputStream);

    }

    private void processPingFrame(int streamId, int flags, ByteBuffer payload) throws IOException {
        if (streamId != 0) {
            // PING frames must be associated with stream 0
            sendGoAway(Http2Frame.PROTOCOL_ERROR);
            return;
        }

        boolean isAck = (flags & Http2Frame.FLAG_ACK) != 0;

        if (!isAck) {
            payload.rewind(); // Rest position to beginning of the buffer
            PingFrame pingAckFrame = new PingFrame(true, payload);
            connectionManager.sendFrame(pingAckFrame, outputStream);
        }
    }

    private void processGoAwayFrame(int streamId, int flags, ByteBuffer payload) {
        if (streamId != 0) {
            // GOAWAY frames must be associated with stream 0
            return;
        }

        GoAwayFrame goAwayFrame = new GoAwayFrame(streamId, flags, payload);
        int lastStreamId = goAwayFrame.getLastStreamId();
        int errorCode = goAwayFrame.getErrorCode();

        System.out.println("Received GOAWAY, last stream: " + lastStreamId + ", error: " + errorCode);

        // Handle connection shutdown
        connectionManager.getAllStreams().forEach(stream -> {
            if (stream.getStreamId() > lastStreamId) {
                try {
                    sendRstStream(stream.getStreamId(), Http2Frame.REFUSED_STREAM);
                } catch (IOException e) {
                    System.out.println("Error resetting stream during GOAWAY: " + e.getMessage());
                }
            }
        });

        connectionManager.markGoAwayReceived();
    }

    private void processWindowUpdateFrame(int streamId, int flags, ByteBuffer payload) {
        WindowUpdateFrame frame = new WindowUpdateFrame(streamId, flags, payload);
        int increment = frame.getWindowSizeIncrement();

        if (streamId == 0) {
            // Connection-level flow control
            connectionManager.increaseConnectionWindow(increment);
        } else {
            // Stream-level flow control
            Http2Stream stream = connectionManager.getStream(streamId);
            if (stream != null) {
                stream.increaseRemoteWindow(increment);
            }
        }
    }

    private void processContinuationFrame(int streamId, int flags, ByteBuffer payload) throws IOException {
        // CONTINUATION frames are part of HEADERS sequence
        if (streamId == 0) {
            sendGoAway(Http2Frame.PROTOCOL_ERROR);
            return;
        }

        Http2Stream stream = connectionManager.getStream(streamId);
        if (stream == null) {
            sendRstStream(streamId, Http2Frame.STREAM_CLOSED);
            return;
        }

        boolean endHeaders = (flags & Http2Frame.FLAG_END_HEADERS) != 0;

        // Process header fragment
        byte[] headerBlock = new byte[payload.remaining()];
        payload.get(headerBlock);

        try {
            ByteArrayInputStream headerInputStream = new ByteArrayInputStream(headerBlock);

            HashMap<String, String> headers = new HashMap<>();
            connectionManager.getDecoder().decode(headerInputStream, (name, value, sensitive) -> {
                String nameStr = new String(name, StandardCharsets.UTF_8);
                String valueStr = new String(value, StandardCharsets.UTF_8);
                headers.put(nameStr, valueStr);
            });

            stream.receiveHeaders(headers, false);

            if (endHeaders && stream.isEndStreamReceived()) {
                createResponse(stream);
            }
        } catch (Exception e) {
            sendRstStream(streamId, Http2Frame.INTERNAL_ERROR);
            System.err.println("Error processing CONTINUATION frame: " + e.getMessage());
        }
    }

    private HttpResponse processRstStreamFrame(int streamId, ByteBuffer payload) {
        if (streamId == 0) {
            // Invalid - RST_STREAM frames cannot be associated with stream 0
            return null;
        }

        RstStreamFrame frame = new RstStreamFrame(streamId, 0, payload);
        int errorCode = frame.getErrorCode();

        Http2Stream stream = connectionManager.getStream(streamId);
        if (stream != null) {
            stream.resetStream(errorCode);
            connectionManager.removeStream(streamId);
        }

        return null;
    }

    private void sendGoAway(int errorCode) throws IOException {
        if (this.outputStream == null) {
            System.err.println("Cannot send GOAWAY: output stream is null");
            return;
        }

        // Get last processed stream ID
        int lastStreamId = 0;
        GoAwayFrame goAwayFrame = new GoAwayFrame(lastStreamId, errorCode);
        connectionManager.sendFrame(goAwayFrame, this.outputStream);
    }

    private void sendRstStream(int streamId, int errorCode) throws IOException {
        if (this.outputStream == null) {
            System.err.println("Cannot seand RST_STREAM: output stream is null");
            return;
        }
        RstStreamFrame rstStreamFrame = new RstStreamFrame(streamId, errorCode);
        connectionManager.sendFrame(rstStreamFrame, this.outputStream);
    }

    private HttpResponse createResponse(Http2Stream stream) {
        // Convert HTTP/2 stream to HTTP request
        Map<String, String> headers = stream.getRequestHeaders();
        ByteBuffer data = stream.getData();

        // Extract method, urlPath, etc. from headers
        String method = headers.get(":method");
        String urlPath = headers.get(":path");
        String scheme = headers.get(":scheme");
        String authority = headers.get(":authority");

        if (method == null || urlPath == null || scheme == null) {
            try {
                sendRstStream(stream.getStreamId(), Http2Frame.PROTOCOL_ERROR);
            } catch (IOException e) {
                System.err.println("Error sending RST_STREAM: " + e.getMessage());
            }
            return null;
        }

        // Add headers
        HashMap<String, String> requestHeaders = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(":")) {
                requestHeaders.put(name, entry.getValue());
            }
        }

        System.out.println("HTTP/2 Request: " + method + " " + urlPath);

        // Create an HttpRequest object
        HttpRequest request = new HttpRequest(method, "HTTP/2", urlPath, requestHeaders);

        // Add request body if present
        if (data.hasRemaining()) {
            byte[] body = new byte[data.remaining()];
            data.get(body);
        }

        // Process the request to get a response
        HttpResponse response = processRequest(request);

        response.setProperty("streamId", stream.getStreamId());

        return response;
    }

    public HttpResponse processRequest(HttpRequest request) {
        try {
            Processor http1Processor = new Processor();
            HttpResponse response = http1Processor.processRequest(request);

            response.setHeader("x-protocol", "HTTP/2");

            return response;
        } catch (Exception e) {
            HttpResponse response = new HttpResponse(request.getProtocolVersion());

            response.setStatusCode("500 Internal Server Error");
            response.setHeader("Content-Type", "text/plain");
            response.setBody("Error processing HTTP/2 request: " + e.getMessage());
            return response;
        }
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
