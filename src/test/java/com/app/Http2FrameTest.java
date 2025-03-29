package com.app;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;

public class Http2FrameTest {

    /**
     * Test implementation of Http2Frame for testing abstract class methods
     */
    private static class TestFrame extends Http2Frame {
        private final int type;

        public TestFrame(int streamId, int flags, int type) {
            super(streamId, flags);
            this.type = type;
        }

        public TestFrame(int streamId, int flags, ByteBuffer payload, int type) {
            super(streamId, flags, payload);
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    @Test
    @DisplayName("Constructor with streamId and flags should initialize fields correctly")
    public void testConstructorWithStreamIdAndFlags() {
        int streamId = 123;
        int flags = 0x5;
        int type = Http2Frame.TYPE_DATA;

        TestFrame frame = new TestFrame(streamId, flags, type);

        assertEquals(streamId, frame.getStreamId());
        assertEquals(flags, frame.getFlags());
        assertEquals(type, frame.getType());
        assertEquals(0, frame.getPayload().remaining()); // Empty payload
    }

    @Test
    @DisplayName("Constructor with payload should initialize fields correctly")
    public void testConstructorWithPayload() {
        int streamId = 123;
        int flags = 0x5;
        int type = Http2Frame.TYPE_HEADERS;
        ByteBuffer payload = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });

        TestFrame frame = new TestFrame(streamId, flags, payload, type);

        assertEquals(streamId, frame.getStreamId());
        assertEquals(flags, frame.getFlags());
        assertEquals(type, frame.getType());

        ByteBuffer framePayload = frame.getPayload();
        assertEquals(payload.remaining(), framePayload.remaining());

        payload.mark();
        byte[] expectedBytes = new byte[payload.remaining()];
        payload.get(expectedBytes);
        payload.reset();

        byte[] actualBytes = new byte[framePayload.remaining()];
        framePayload.get(actualBytes);

        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    @DisplayName("getPayload should return a duplicate of the payload")
    public void testGetPayloadReturnsDuplicate() {
        ByteBuffer originalPayload = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        TestFrame frame = new TestFrame(1, 0, originalPayload, Http2Frame.TYPE_DATA);

        ByteBuffer payload1 = frame.getPayload();
        ByteBuffer payload2 = frame.getPayload();

        assertNotSame(payload1, payload2);

        assertEquals(payload1.remaining(), payload2.remaining());

        payload1.mark();
        payload2.mark();

        while (payload1.hasRemaining()) {
            assertEquals(payload1.get(), payload2.get());
        }

        payload1.reset();
        payload2.reset();

        payload1.position(payload1.position() + 2);
        assertEquals(0, payload2.position());

        payload2.position(payload2.position() + 3);
        ByteBuffer payload3 = frame.getPayload();
        assertEquals(0, payload3.position());
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Http2Frame.FLAG_ACK,
            Http2Frame.FLAG_END_STREAM,
            Http2Frame.FLAG_END_HEADERS,
            Http2Frame.FLAG_PADDED,
            Http2Frame.FLAG_PRIORITY
    })
    @DisplayName("hasFlag should detect individual flags correctly")
    public void testHasFlag(int flag) {
        TestFrame frameWithFlag = new TestFrame(1, flag, Http2Frame.TYPE_DATA);
        assertTrue(frameWithFlag.hasFlag(flag));

        TestFrame frameWithoutFlag = new TestFrame(1, 0, Http2Frame.TYPE_DATA);
        assertFalse(frameWithoutFlag.hasFlag(flag));

        TestFrame frameWithMultipleFlags = new TestFrame(1, flag | 0x10, Http2Frame.TYPE_DATA);
        assertTrue(frameWithMultipleFlags.hasFlag(flag));
    }

    @Test
    @DisplayName("encode should properly serialize frame to wire format")
    public void testEncode() {
        int streamId = 123;
        int flags = 0x5;
        int type = Http2Frame.TYPE_DATA;
        byte[] payloadBytes = { 10, 20, 30, 40, 50 };
        ByteBuffer payload = ByteBuffer.wrap(payloadBytes);

        TestFrame frame = new TestFrame(streamId, flags, payload, type);
        ByteBuffer encodedFrame = frame.encode();

        assertEquals(9 + payloadBytes.length, encodedFrame.remaining());
        assertEquals(payloadBytes.length, (encodedFrame.get(0) & 0xFF) << 16 |
                (encodedFrame.get(1) & 0xFF) << 8 |
                (encodedFrame.get(2) & 0xFF));

        assertEquals(type, encodedFrame.get(3) & 0xFF);
        assertEquals(flags, encodedFrame.get(4) & 0xFF);

        assertEquals(streamId, (encodedFrame.get(5) & 0x7F) << 24 |
                (encodedFrame.get(6) & 0xFF) << 16 |
                (encodedFrame.get(7) & 0xFF) << 8 |
                (encodedFrame.get(8) & 0xFF));

        byte[] encodedPayload = new byte[payloadBytes.length];
        encodedFrame.position(9); // Skip header
        encodedFrame.get(encodedPayload);

        assertArrayEquals(payloadBytes, encodedPayload);
    }

    @Test
    @DisplayName("encode should handle empty payload correctly")
    public void testEncodeWithEmptyPayload() {
        TestFrame frame = new TestFrame(1, 0, Http2Frame.TYPE_PING);
        ByteBuffer encodedFrame = frame.encode();

        assertEquals(9, encodedFrame.remaining()); // Just the header
        assertEquals(0, encodedFrame.get(0) & 0xFF); // Length is 0
        assertEquals(0, encodedFrame.get(1) & 0xFF);
        assertEquals(0, encodedFrame.get(2) & 0xFF);
    }

    @Test
    @DisplayName("parse should throw exception for buffer smaller than frame header")
    public void testParseWithBufferTooSmall() {
        ByteBuffer tooSmallBuffer = ByteBuffer.allocate(8); // Less than 9 bytes header

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Http2Frame.parse(tooSmallBuffer));

        assertTrue(exception.getMessage().contains("too small"));
    }

    @Test
    @DisplayName("parse should throw exception for buffer smaller than header + payload")
    public void testParseWithBufferTooSmallForPayload() {
        ByteBuffer buffer = ByteBuffer.allocate(10); // 9 byte header + 1 byte payload

        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x05); // Payload length = 5 bytes
        buffer.put((byte) Http2Frame.TYPE_DATA);
        buffer.put((byte) 0x00); // Flags
        buffer.put((byte) 0x00); // Stream ID bytes
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0xFF); // Only 1 byte of payload

        buffer.flip();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Http2Frame.parse(buffer));

        assertTrue(exception.getMessage().contains("too small"));
    }

    @Test
    @DisplayName("parse should throw exception for unknown frame type")
    public void testParseWithUnknownFrameType() {
        ByteBuffer buffer = ByteBuffer.allocate(9); // Just header, no payload

        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00); // No payload
        buffer.put((byte) 0xFF); // Unknown type
        buffer.put((byte) 0x00); // Flags
        buffer.put((byte) 0x00); // Stream ID bytes
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);

        buffer.flip();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Http2Frame.parse(buffer));

        assertTrue(exception.getMessage().contains("Unknown frame type"));
    }

    @Test
    @DisplayName("parse should correctly deserialize DATA frame")
    public void testParseDataFrame() {
        int streamId = 5;
        int flags = Http2Frame.FLAG_END_STREAM;
        byte[] payload = { 1, 2, 3, 4 };

        ByteBuffer buffer = ByteBuffer.allocate(9 + payload.length);

        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) payload.length); // Payload length
        buffer.put((byte) Http2Frame.TYPE_DATA); // Type
        buffer.put((byte) flags); // Flags
        buffer.put((byte) ((streamId >> 24) & 0x7F)); // Stream ID
        buffer.put((byte) ((streamId >> 16) & 0xFF));
        buffer.put((byte) ((streamId >> 8) & 0xFF));
        buffer.put((byte) (streamId & 0xFF));

        buffer.put(payload);
        buffer.flip();

        Http2Frame parsedFrame = Http2Frame.parse(buffer);

        assertTrue(parsedFrame instanceof DataFrame);
        assertEquals(streamId, parsedFrame.getStreamId());
        assertEquals(flags, parsedFrame.getFlags());
        assertEquals(Http2Frame.TYPE_DATA, parsedFrame.getType());
        assertTrue(parsedFrame.hasFlag(Http2Frame.FLAG_END_STREAM));

        ByteBuffer parsedPayload = parsedFrame.getPayload();
        assertEquals(payload.length, parsedPayload.remaining());

        byte[] parsedPayloadBytes = new byte[parsedPayload.remaining()];
        parsedPayload.get(parsedPayloadBytes);

        assertArrayEquals(payload, parsedPayloadBytes);
    }

    @Test
    @DisplayName("encode then parse should produce equivalent frame")
    public void testEncodeThenParse() {
        int streamId = 7;
        int flags = Http2Frame.FLAG_END_HEADERS;
        byte[] payloadBytes = { 5, 10, 15, 20, 25 };
        ByteBuffer payload = ByteBuffer.wrap(payloadBytes);

        HeadersFrame originalFrame = new HeadersFrame(streamId, flags, payload);
        ByteBuffer encodedFrame = originalFrame.encode();
        Http2Frame parsedFrame = Http2Frame.parse(encodedFrame);

        assertTrue(parsedFrame instanceof HeadersFrame);
        assertEquals(originalFrame.getStreamId(), parsedFrame.getStreamId());
        assertEquals(originalFrame.getFlags(), parsedFrame.getFlags());
        assertEquals(originalFrame.getType(), parsedFrame.getType());

        ByteBuffer originalPayload = originalFrame.getPayload();
        ByteBuffer parsedPayload = parsedFrame.getPayload();

        assertEquals(originalPayload.remaining(), parsedPayload.remaining());

        originalPayload.mark();
        parsedPayload.mark();

        while (originalPayload.hasRemaining()) {
            assertEquals(originalPayload.get(), parsedPayload.get());
        }
    }
}
