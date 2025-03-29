package com.app;

import java.nio.ByteBuffer;

/**
 * PING frame (type=0x6) is used for measuring round-trip time and checking
 * connection liveliness
 */
public class PingFrame extends Http2Frame {

    public PingFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public PingFrame(boolean ack, ByteBuffer data) {
        super(0, ack ? FLAG_ACK : 0);

        ByteBuffer buffer = ByteBuffer.allocate(8);

        if (data != null && data.remaining() >= 8) {
            // Copy up to 8 bytes from the data
            byte[] bytes = new byte[8];
            data.get(bytes);
            buffer.put(bytes);
        }

        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_PING;
    }

    public boolean isAck() {
        return hasFlag(FLAG_ACK);
    }
}
