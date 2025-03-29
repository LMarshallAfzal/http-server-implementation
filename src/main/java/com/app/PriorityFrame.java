package com.app;

import java.nio.ByteBuffer;

/**
 * PRIORITY frame (type=0x2) specifies the sender-advised priority of a stream.
 */
public class PriorityFrame extends Http2Frame {

    public PriorityFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public PriorityFrame(int streamId, int dependencyStreamId, boolean exclusive, int weight) {
        super(streamId, 0);

        ByteBuffer buffer = ByteBuffer.allocate(5);

        // Set first bit to 1 if exclusive, leave as 0 if not
        int streamDependency = exclusive ? (dependencyStreamId | 0x80000000) : dependencyStreamId;

        buffer.putInt(streamDependency);
        buffer.put((byte) (weight - 1)); // Weight is 1 less than the value (1-256 mapped to 0-255)

        buffer.flip();
        this.payload = buffer;
    }

    @Override
    public int getType() {
        return TYPE_PRIORITY;
    }

    public int getDependencyStreamId() {
        int originalPosition = payload.position();
        payload.position(0);
        int streamDependency = payload.getInt() & 0x7FFFFFFF; // Clear the exclusive bit
        payload.position(originalPosition);
        return streamDependency;
    }

    public boolean isExclusive() {
        int originalPosition = payload.position();
        payload.position(0);
        int firstInt = payload.getInt();
        payload.position(originalPosition);
        return (firstInt & 0x80000000) != 0; // Check if first bit is set
    }

    public int getWeight() {
        int originalPosition = payload.position();
        payload.position(4); // Position after the stream dependency
        int weight = (payload.get() & 0xFF) + 1; // Weight is stored as value-1
        payload.position(originalPosition);
        return weight;
    }
}
