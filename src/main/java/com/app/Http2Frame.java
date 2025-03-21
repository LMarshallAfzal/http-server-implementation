package com.app;

import java.nio.ByteBuffer;

public abstract class Http2Frame {
    // Frame type constants
    public static final int TYPE_DATA = 0x0;
    public static final int TYPE_HEADERS = 0x1;
    public static final int TYPE_PRIORITY = 0x2;
    public static final int TYPE_RST_STREAM = 0x3;
    public static final int TYPE_SETTINGS = 0x4;
    public static final int TYPE_PUSH_PROMISE = 0x5;
    public static final int TYPE_PING = 0x6;
    public static final int TYPE_GOAWAY = 0x7;
    public static final int TYPE_WINDOW_UPDATE = 0x8;
    public static final int TYPE_CONTINUATION = 0x9;

    // Frame flags
    public static final int FLAG_ACK = 0x1;
    public static final int FLAG_END_STREAM = 0x1;
    public static final int FLAG_END_HEADERS = 0x4;
    public static final int FLAG_PADDED = 0x8;
    public static final int FLAG_PRIORITY = 0x20;

    // Error codes
    public static final int NO_ERROR = 0x0;
    public static final int PROTOCOL_ERROR = 0x1;
    public static final int INTERNAL_ERROR = 0x2;
    public static final int FLOW_CONTROL_ERROR = 0x3;
    public static final int SETTINGS_TIMEOUT = 0x4;
    public static final int STREAM_CLOSED = 0x5;
    public static final int FRAME_SIZE_ERROR = 0x6;
    public static final int REFUSED_STREAM = 0x7;
    public static final int CANCEL = 0x8;
    public static final int COMPRESSION_ERROR = 0x9;
    public static final int CONNECT_ERROR = 0xa;
    public static final int ENHANCE_YOUR_CALM = 0xb;
    public static final int INADEQUATE_SECURITY = 0xc;
    public static final int HTTP_1_1_REQUIRED = 0xd;

    protected int streamId;
    protected int flags;
    protected ByteBuffer payload;

    public Http2Frame(int streamId, int flags) {
        this.streamId = streamId;
        this.flags = flags;
        this.payload = ByteBuffer.allocate(0);
    }

    public Http2Frame(int streamId, int flags, ByteBuffer payload) {
        this.streamId = streamId;
        this.flags = flags;
        this.payload = payload;
    }

    public int getStreamId() {
        return streamId;
    }

    public int getFlags() {
        return flags;
    }

    public ByteBuffer getPayload() {
        return payload.duplicate();
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    public abstract int getType();

    /**
     * Encode the frame into a ByteBuffer ready to be sent over the wire.
     * Format: 24 bits length + 8 bits type + 8 bits flags + 1 bit reserved + 31
     * bits stream ID + payload
     */
    public ByteBuffer encode() {
        // Calculate the frame size (payload length)
        int length = payload.remaining();

        // Allocate a buffer for the frame header (9 bytes) + payload
        ByteBuffer buffer = ByteBuffer.allocate(9 + length);

        // Write the length (24 bits)
        buffer.put((byte) ((length >> 16) & 0xFF));
        buffer.put((byte) ((length >> 8) & 0xFF));
        buffer.put((byte) (length & 0xFF));

        // Write the type (8 bits)
        buffer.put((byte) getType());

        // Write the flags (8 bits)
        buffer.put((byte) flags);

        // Write the stream ID (31 bits) with reserved bit (1 bit) set to 0
        buffer.put((byte) ((streamId >> 24) & 0x7F)); // Top bit is reserved (0)
        buffer.put((byte) ((streamId >> 16) & 0xFF));
        buffer.put((byte) ((streamId >> 8) & 0xFF));
        buffer.put((byte) (streamId & 0xFF));

        // Write the payload
        payload.mark();
        buffer.put(payload);
        payload.reset();

        // Prepare the buffer for reading
        buffer.flip();

        return buffer;
    }

    /**
     * Parse a frame from a ByteBuffer.
     */
    public static Http2Frame parse(ByteBuffer buffer) {
        if (buffer.remaining() < 9) {
            throw new IllegalArgumentException("Buffer too small to contain an HTTP/2 frame header");
        }

        // Read the length (24 bits)
        int length = (buffer.get() & 0xFF) << 16 | (buffer.get() & 0xFF) << 8 | (buffer.get() & 0xFF);

        // Read the type (8 bits)
        int type = buffer.get() & 0xFF;

        // Read the flags (8 bits)
        int flags = buffer.get() & 0xFF;

        // Read the stream ID (31 bits), ignoring the reserved bit
        int streamId = (buffer.get() & 0x7F) << 24 | (buffer.get() & 0xFF) << 16 |
                (buffer.get() & 0xFF) << 8 | (buffer.get() & 0xFF);

        // Read the payload
        if (buffer.remaining() < length) {
            throw new IllegalArgumentException("Buffer too small to contain the frame payload");
        }

        // Extract the payload
        byte[] payloadBytes = new byte[length];
        buffer.get(payloadBytes);
        ByteBuffer payload = ByteBuffer.wrap(payloadBytes);

        // Create the appropriate frame type
        switch (type) {
            case TYPE_DATA:
                return new DataFrame(streamId, flags, payload);
            case TYPE_HEADERS:
                return new HeadersFrame(streamId, flags, payload);
            // case TYPE_PRIORITY:
            // return new PriorityFrame(streamId, flags, payload);
            case TYPE_RST_STREAM:
                return new RstStreamFrame(streamId, flags, payload);
            case TYPE_SETTINGS:
                return new SettingsFrame(streamId, flags, payload);
            // case TYPE_PUSH_PROMISE:
            // return new PushPromiseFrame(streamId, flags, payload);
            // case TYPE_PING:
            // return new PingFrame(streamId, flags, payload);
            case TYPE_GOAWAY:
                return new GoAwayFrame(streamId, flags, payload);
            case TYPE_WINDOW_UPDATE:
                return new WindowUpdateFrame(streamId, flags, payload);
            // case TYPE_CONTINUATION:
            // return new ContinuationFrame(streamId, flags, payload);
            default:
                throw new IllegalArgumentException("Unknown frame type: " + type);
        }
    }
}
