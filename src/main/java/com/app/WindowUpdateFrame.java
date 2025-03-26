package com.app;

import java.nio.ByteBuffer;

/**
 * WINDOW_UPDATE frame (type=0x8) is used for flow control.
 */
class WindowUpdateFrame extends Http2Frame {
    public WindowUpdateFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public WindowUpdateFrame(int streamId, int windowSizeIncrement) {
        super(streamId, 0);

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(windowSizeIncrement & 0x7FFFFFFF); // 31-bit value
        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_WINDOW_UPDATE;
    }

    public int getWindowSizeIncrement() {
        int originalPosition = payload.position();
        // payload.mark();
        payload.rewind();
        int increment = payload.getInt() & 0x7FFFFFFF; // 31-bit value
        // payload.reset();
        payload.position(originalPosition);
        return increment;
    }
}
