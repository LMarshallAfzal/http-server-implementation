package com.app;

import java.nio.ByteBuffer;

/**
 * DATA frame (type=0x0) contains the application data.
 */
public class DataFrame extends Http2Frame {
    public DataFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public DataFrame(int streamId, ByteBuffer data, boolean endStream) {
        super(streamId, endStream ? FLAG_END_STREAM : 0, data);
    }

    @Override
    public int getType() {
        return TYPE_DATA;
    }

    public boolean isEndStream() {
        return hasFlag(FLAG_END_STREAM);
    }
}
