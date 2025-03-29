package com.app;

import java.nio.ByteBuffer;

/**
 * HEADERS frame (type=0x1) contains the HTTP header fields.
 */
class HeadersFrame extends Http2Frame {
    public HeadersFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public HeadersFrame(int streamId, ByteBuffer encodedHeaders, boolean endStream, boolean endHeaders) {
        super(streamId,
                (endStream ? FLAG_END_STREAM : 0) | (endHeaders ? FLAG_END_HEADERS : 0),
                encodedHeaders);
    }

    @Override
    public int getType() {
        return TYPE_HEADERS;
    }

    public boolean isEndStream() {
        return hasFlag(FLAG_END_STREAM);
    }

    public boolean isEndHeaders() {
        return hasFlag(FLAG_END_HEADERS);
    }

    public boolean hasPriority() {
        return hasFlag(FLAG_PRIORITY);
    }
}
