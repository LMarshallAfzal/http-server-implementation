package com.app;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

enum State {
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
    private State state;

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
    private boolean endStreamREceived;
    private boolean headersSent;
    private boolean headersReceived;

    private int errorCode;

    public Http2Stream(int streamId, Http2ConnectionManager connectionManager) {
        this.streamId = streamId;
        this.connectionManager = connectionManager;
        this.state = State.IDLE;

        this.weight = 16;
        this.localWindowSize = connectionManager.getLocalSettings().getInitialWindowSize();
        this.remoteWindowSize = connectionManager.getRemoteSettings().getInitialWindowSize();
        this.dataBuffer = ByteBuffer.allocate(16384);

        this.endStreamSent = false;
        this.endStreamRecieved = false;
        this.headersSent = false;
        this.headersReceived = false;
    }

    public synchronized boolean transitionToOpen() {
        if (state == State.IDLE) {
            state = State.OPEN;
            return true;
        }
        return false;
    }

}
