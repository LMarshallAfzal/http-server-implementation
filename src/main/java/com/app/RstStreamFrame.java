package com.app;

import java.nio.ByteBuffer;

/**
 * RST_STREAM frame (type=0x3) terminates a stream.
 */
class RstStreamFrame extends Http2Frame {
    public RstStreamFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public RstStreamFrame(int streamId, int errorCode) {
        super(streamId, 0);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(errorCode);
        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_RST_STREAM;
    }

    public int getErrorCode() {
        int originalPosition = payload.position();
        // payload.mark();
        payload.rewind();
        int errorCode = payload.getInt();
        // payload.reset();
        payload.position(originalPosition);
        return errorCode;
    }
}
