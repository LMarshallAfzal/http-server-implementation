package com.app;

import java.nio.ByteBuffer;

/**
 * CONTINUATION frame (type=0x9) is used to continue a sequence of header block
 * fragments.
 */
public class ContinuationFrame extends Http2Frame {

    public ContinuationFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public ContinuationFrame(int streamId, ByteBuffer headerBlockFragment, boolean endHeaders) {
        super(streamId, endHeaders ? FLAG_END_HEADERS : 0, headerBlockFragment);
    }

    @Override
    public int getType() {
        return TYPE_CONTINUATION;
    }

    public boolean isEndHeaders() {
        return hasFlag(FLAG_END_HEADERS);
    }
}
