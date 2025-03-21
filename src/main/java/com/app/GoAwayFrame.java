package com.app;

import java.nio.ByteBuffer;

/**
 * GOAWAY frame (type=0x7) is used to initiate shutdown of a connection.
 */
class GoAwayFrame extends Http2Frame {
    public GoAwayFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public GoAwayFrame(int lastStreamId, int errorCode) {
        super(0, 0); // GOAWAY frames are always sent on stream 0

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(lastStreamId & 0x7FFFFFFF); // 31-bit value
        buffer.putInt(errorCode);
        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_GOAWAY;
    }

    public int getLastStreamId() {
        payload.mark();
        payload.rewind();
        int lastStreamId = payload.getInt() & 0x7FFFFFFF; // 31-bit value
        payload.reset();
        return lastStreamId;
    }

    public int getErrorCode() {
        payload.mark();
        payload.rewind();
        payload.getInt(); // Skip last stream ID
        int errorCode = payload.getInt();
        payload.reset();
        return errorCode;
    }
}
