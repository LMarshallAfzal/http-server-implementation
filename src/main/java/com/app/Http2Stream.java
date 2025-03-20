package com.app;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;
import java.io.IOException;

enum StreamState {
    IDLE,
    RESERVED_LOCAL,
    RESERVED_REMOTE,
    OPEN,
    HALF_CLOSED_LOCAL,
    HALF_CLOSED_REMOTE,
    CLOSED
}

public class Http2Stream {

    private int streamId;
    private StreamState state;

    private int weight;
    private boolean exclusive;
    private Http2Stream parent;
    private final HashSet<Http2Stream> children = new HashSet<>();

    private int localWindowSize; // Our window for receiving data
    private int remoteWindowSize; // Peer's window for receiving data

    private final HashMap<String, String> requestHeaders = new HashMap<>();
    private final HashMap<String, String> responseHeaders = new HashMap<>();
    private final HashMap<String, String> trailers = new HashMap<>();

    private ByteBuffer dataBuffer;

    private final Http2ConnectionManager connectionManager;

    private boolean endStreamSent;
    private boolean endStreamReceived;
    private boolean headersSent;
    private boolean headersReceived;

    private int errorCode;

    public Http2Stream(int streamId, Http2ConnectionManager connectionManager) {
        this.streamId = streamId;
        this.connectionManager = connectionManager;
        this.state = StreamState.IDLE;

        this.weight = 16;
        this.localWindowSize = connectionManager.getLocalSettings().getInitialWindowSize();
        this.remoteWindowSize = connectionManager.getRemoteSettings().getInitialWindowSize();
        this.dataBuffer = ByteBuffer.allocate(16384);

        this.endStreamSent = false;
        this.endStreamReceived = false;
        this.headersSent = false;
        this.headersReceived = false;
    }

    // State transition methods
    public synchronized boolean transitionToOpen() {
        if (state == StreamState.IDLE) {
            state = StreamState.OPEN;
            return true;
        }
        return false;
    }

    public synchronized boolean closeLocal() {
        if (state == StreamState.OPEN) {
            state = StreamState.HALF_CLOSED_LOCAL;
            return true;
        } else if (state == StreamState.HALF_CLOSED_REMOTE) {
            state = StreamState.CLOSED;
            return true;
        }
        return false;
    }

    public synchronized boolean closeRemote() {
        if (state == StreamState.OPEN) {
            state = StreamState.HALF_CLOSED_REMOTE;
            return true;
        } else if (state == StreamState.HALF_CLOSED_LOCAL) {
            state = StreamState.CLOSED;
            return true;
        }
        return false;
    }

    public synchronized void resetStream(int errorCode) {
        this.errorCode = errorCode;
        state = StreamState.CLOSED;
    }

    // Flow control methods
    public synchronized boolean consumeLocalWindow(int size) {
        if (localWindowSize >= size) {
            localWindowSize -= size;
            return true;
        }
        return false;
    }

    public synchronized void increaseLocalWindow(int increment) {
        localWindowSize += increment;
    }

    public synchronized boolean consumeRemoteWindow(int size) {
        if (remoteWindowSize >= size) {
            remoteWindowSize -= size;
            return true;
        }
        return false;
    }

    public synchronized void increaseRemoteWindow(int increment) {
        remoteWindowSize += increment;
    }

    // Priority handling
    public void setPriority(int weight, boolean exclusive, Http2Stream parent) {
        this.weight = weight;
        this.exclusive = exclusive;

        if (this.parent != null) {
            this.parent.children.remove(this);
        }

        if (parent != null) {
            this.parent = parent;
            parent.children.add(this);

            if (exclusive) {
                // Move all siblings to be children of this stream
                for (Http2Stream sibling : new ArrayList<>(parent.children)) {
                    if (sibling != this) {
                        parent.children.remove(sibling);
                        this.children.add(sibling);
                        sibling.parent = this;
                    }
                }
            }
        }
    }

    // Header methods
    public void addRequestHeader(String name, String value) {
        requestHeaders.put(name.toLowerCase(), value);
    }

    public void addResponseHeader(String name, String value) {
        responseHeaders.put(name.toLowerCase(), value);
    }

    public void addTrailer(String name, String value) {
        trailers.put(name.toLowerCase(), value);
    }

    public String getRequestHeader(String name) {
        return requestHeaders.get(name.toLowerCase());
    }

    public String getResponseHeader(String name) {
        return responseHeaders.get(name.toLowerCase());
    }

    public HashMap<String, String> getRequestHeaders() {
        return new HashMap<>(requestHeaders);
    }

    public HashMap<String, String> getResponseHeaders() {
        return new HashMap<>(responseHeaders);
    }

    // Data handling
    public void appendData(ByteBuffer data) {
        // Resize buffer if needed
        if (dataBuffer.remaining() < data.remaining()) {
            int newSize = dataBuffer.capacity() + Math.max(data.remaining(), 8192);
            ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
            dataBuffer.flip();
            newBuffer.put(dataBuffer);
            dataBuffer = newBuffer;
        }

        dataBuffer.put(data);
    }

    public ByteBuffer getData() {
        ByteBuffer copy = dataBuffer.duplicate();
        copy.flip();
        return copy;
    }

    public void clearData() {
        dataBuffer.clear();
    }

    // Frame processing methods - these would depend on your Http2Frame
    // implementation
    public void receiveHeaders(HashMap<String, String> headers, boolean endStream) {
        headers.forEach((name, value) -> {
            if (headersReceived) {
                addTrailer(name, value);
            } else {
                addRequestHeader(name, value);
            }
        });

        headersReceived = true;

        if (endStream) {
            endStreamReceived = true;
            closeRemote();
        }
    }

    public void receiveData(ByteBuffer data, boolean endStream) {
        appendData(data);

        if (endStream) {
            endStreamReceived = true;
            closeRemote();
        }
    }

    // Frame sending methods - these would depend on your Http2Frame implementation
    public void sendHeaders(HashMap<String, String> headers, boolean endStream) throws IOException {
        // Implementation would encode headers and send via connection manager
        // For example: connectionManager.sendFrame(new HeadersFrame(streamId, headers,
        // endStream));

        headers.forEach(this::addResponseHeader);
        headersSent = true;

        if (endStream) {
            endStreamSent = true;
            closeLocal();
        }
    }

    public void sendData(ByteBuffer data, boolean endStream) throws IOException {
        // Implementation would split data into appropriate sized frames and send
        // For example: connectionManager.sendFrame(new DataFrame(streamId, data,
        // endStream));

        if (endStream) {
            endStreamSent = true;
            closeLocal();
        }
    }

    public void sendRstStream(int errorCode) throws IOException {
        // Implementation would create and send RST_STREAM frame
        // For example: connectionManager.sendFrame(new RstStreamFrame(streamId,
        // errorCode));

        this.errorCode = errorCode;
        state = StreamState.CLOSED;
    }

    public void sendWindowUpdate(int increment) throws IOException {
        // Implementation would send a WINDOW_UPDATE frame
        // For example: connectionManager.sendFrame(new WindowUpdateFrame(streamId,
        // increment));

        increaseRemoteWindow(increment);
    }

    // Getters and utility methods
    public int getStreamId() {
        return streamId;
    }

    public StreamState getState() {
        return state;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public Http2Stream getParent() {
        return parent;
    }

    public Set<Http2Stream> getChildren() {
        return Collections.unmodifiableSet(children);
    }

    public int getLocalWindowSize() {
        return localWindowSize;
    }

    public int getRemoteWindowSize() {
        return remoteWindowSize;
    }

    public boolean isEndStreamSent() {
        return endStreamSent;
    }

    public boolean isEndStreamReceived() {
        return endStreamReceived;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isActive() {
        return state == StreamState.OPEN ||
                state == StreamState.HALF_CLOSED_LOCAL ||
                state == StreamState.HALF_CLOSED_REMOTE;
    }
}
