package com.app;

import java.nio.ByteBuffer;

/**
 * PUSH_PROMISE frame (type=0x5) is used to notify the peer that the server
 * intends
 * to initiate a stream.
 */
public class PushPromiseFrame extends Http2Frame {

    public PushPromiseFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public PushPromiseFrame(int streamId, int promisedStreamId, ByteBuffer headerBlock, boolean endHeaders) {
        super(streamId, endHeaders ? FLAG_END_HEADERS : 0);

        // Create payload: 4 bytes for promised stream ID + header block
        ByteBuffer buffer = ByteBuffer.allocate(4 + headerBlock.remaining());

        // Add promised stream ID (31 bits)
        buffer.putInt(promisedStreamId & 0x7FFFFFFF);

        // Add header block fragment
        headerBlock.mark();
        buffer.put(headerBlock);
        headerBlock.reset();

        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_PUSH_PROMISE;
    }

    public boolean isEndHeaders() {
        return hasFlag(FLAG_END_HEADERS);
    }

    public boolean isPadded() {
        return hasFlag(FLAG_PADDED);
    }

    public int getPromisedStreamId() {
        int originalPosition = payload.position();
        payload.position(0);
        int promisedStreamId = payload.getInt() & 0x7FFFFFFF; // 31-bit value
        payload.position(originalPosition);
        return promisedStreamId;
    }

    /**
     * Get the header block fragment from the payload
     */
    public ByteBuffer getHeaderBlockFragment() {
        int payloadLength = payload.remaining();
        int startPosition = payload.position() + 4; // Skip promised stream ID

        // If the frame is padded, we need to account for the padding length byte
        // and the padding itself
        if (isPadded()) {
            int padLength = payload.get(payload.position()) & 0xFF;
            startPosition += 1; // Skip the padding length byte
            payloadLength -= (padLength + 1); // Subtract padding length and the length byte
        }

        // Create a view of the payload that only includes the header block fragment
        ByteBuffer headerBlock = payload.duplicate();
        headerBlock.position(startPosition);
        headerBlock.limit(startPosition + payloadLength - 4); // Subtract the 4 bytes for promised stream ID

        return headerBlock.slice();
    }
}
