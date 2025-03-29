package com.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class Http2ProcessorTest {

    private Http2ConnectionManager connectionManager;
    private Http2Processor processor;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setUp() {
        connectionManager = new Http2ConnectionManager();
        processor = new Http2Processor(connectionManager);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    @DisplayName("initialise should send SETTINGS frame")
    public void testInitialise() throws IOException {
        // Initialize with our test output stream
        processor.initialise(outputStream);

        // Verify some data was written to the output stream
        byte[] frameData = outputStream.toByteArray();
        assertTrue(frameData.length > 0, "No data was written to the output stream");

        // Parse the frame that was sent
        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        Http2Frame sentFrame = Http2Frame.parse(buffer);

        // Verify it's a SETTINGS frame
        assertEquals(Http2Frame.TYPE_SETTINGS, sentFrame.getType(), "Wrong frame type");
        assertEquals(0, sentFrame.getStreamId(), "Wrong stream ID");
        assertFalse(sentFrame.hasFlag(Http2Frame.FLAG_ACK), "Should not be an ACK");
    }

    // TODO: Fix headerTableSize setting because it is always the default

    // @Test
    // @DisplayName("processSettingsFrame should update remote settings and send
    // ACK")
    // public void testProcessSettingsFrame() throws IOException {
    //// Create a SETTINGS frame with custom values
    // Http2Settings settings = new Http2Settings();
    // settings.setHeaderTableSize(8192);
    // settings.setMaxFrameSize(16384);
    //
    // SettingsFrame settingsFrame = new SettingsFrame(settings);
    // ByteBuffer payload = settingsFrame.getPayload();
    //
    //// Initialize processor
    // processor.initialise(outputStream);
    //
    //// Clear the output stream to ignore the initial SETTINGS
    // outputStream.reset();
    //
    //// Call processSettingsFrame using reflection to access the private method
    // try {
    // Method processSettingsFrameMethod = Http2Processor.class.getDeclaredMethod(
    // "processSettingsFrame", int.class, int.class, ByteBuffer.class);
    // processSettingsFrameMethod.setAccessible(true);
    // processSettingsFrameMethod.invoke(processor, 0, 0, payload);
    //
    //// Verify settings were updated
    // Http2Settings remoteSettings = connectionManager.getRemoteSettings();
    // assertEquals(8192, remoteSettings.getHeaderTableSize(), "Header table size
    // not updated");
    // assertEquals(16384, remoteSettings.getMaxFrameSize(), "Max frame size not
    // updated");
    //
    //// Verify an ACK was sent
    // byte[] frameData = outputStream.toByteArray();
    // assertTrue(frameData.length > 0, "No data was written to the output stream");
    //
    //// Parse the frame that was sent
    // ByteBuffer buffer = ByteBuffer.wrap(frameData);
    // Http2Frame sentFrame = Http2Frame.parse(buffer);
    //
    //// Verify it's a SETTINGS ACK
    // assertEquals(Http2Frame.TYPE_SETTINGS, sentFrame.getType(), "Wrong frame
    // type");
    // assertTrue(sentFrame.hasFlag(Http2Frame.FLAG_ACK), "Should be an ACK");
    //
    // } catch (Exception e) {
    // fail("Failed to access processSettingsFrame method: " + e.getMessage());
    // }
    // }

    @Test
    @DisplayName("processPingFrame should send PING ACK")
    public void testProcessPingFrame() throws IOException {
        // Create a PING frame with test data
        ByteBuffer data = ByteBuffer.allocate(8);
        data.putLong(123456789L);
        data.flip();

        PingFrame pingFrame = new PingFrame(false, data);
        ByteBuffer payload = pingFrame.getPayload();

        // Initialize processor
        processor.initialise(outputStream);

        // Clear the output stream to ignore the initial SETTINGS
        outputStream.reset();

        // Call processPingFrame using reflection
        try {
            Method processPingFrameMethod = Http2Processor.class.getDeclaredMethod(
                    "processPingFrame", int.class, int.class, ByteBuffer.class);
            processPingFrameMethod.setAccessible(true);
            processPingFrameMethod.invoke(processor, 0, 0, payload);

            // Verify a PING ACK was sent
            byte[] frameData = outputStream.toByteArray();
            assertTrue(frameData.length > 0, "No data was written to the output stream");

            // Parse the frame that was sent
            ByteBuffer buffer = ByteBuffer.wrap(frameData);
            Http2Frame sentFrame = Http2Frame.parse(buffer);

            // Verify it's a PING ACK
            assertEquals(Http2Frame.TYPE_PING, sentFrame.getType(), "Wrong frame type");
            assertTrue(sentFrame.hasFlag(Http2Frame.FLAG_ACK), "Should be an ACK");

            // Verify the payload is the same
            ByteBuffer sentPayload = sentFrame.getPayload();
            assertEquals(8, sentPayload.remaining(), "Wrong payload size");
            assertEquals(123456789L, sentPayload.getLong(), "Wrong payload content");

        } catch (Exception e) {
            fail("Failed to access processPingFrame method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("processGoAwayFrame should mark connection as having received GOAWAY")
    public void testProcessGoAwayFrame() throws IOException {
        // Create a GOAWAY frame
        int lastStreamId = 5;
        int errorCode = Http2Frame.PROTOCOL_ERROR;
        GoAwayFrame goAwayFrame = new GoAwayFrame(lastStreamId, errorCode);
        ByteBuffer payload = goAwayFrame.getPayload();

        // Process the frame using reflection
        try {
            Method processGoAwayFrameMethod = Http2Processor.class.getDeclaredMethod(
                    "processGoAwayFrame", int.class, int.class, ByteBuffer.class);
            processGoAwayFrameMethod.setAccessible(true);
            processGoAwayFrameMethod.invoke(processor, 0, 0, payload);

            // Verify connection was marked
            assertTrue(connectionManager.isGoAwayReceived(), "Connection not marked as having received GOAWAY");

        } catch (Exception e) {
            fail("Failed to access processGoAwayFrame method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("processWindowUpdateFrame should increase connection window")
    public void testProcessWindowUpdateFrameConnection() throws IOException {
        // Get initial window size
        int initialSize = getConnectionWindowSize();

        // Create a connection-level WINDOW_UPDATE frame
        int increment = 10000;
        WindowUpdateFrame windowUpdateFrame = new WindowUpdateFrame(0, increment);
        ByteBuffer payload = windowUpdateFrame.getPayload();

        // Process the frame using reflection
        try {
            Method processWindowUpdateFrameMethod = Http2Processor.class.getDeclaredMethod(
                    "processWindowUpdateFrame", int.class, int.class, ByteBuffer.class);
            processWindowUpdateFrameMethod.setAccessible(true);
            processWindowUpdateFrameMethod.invoke(processor, 0, 0, payload);

            // Verify window was increased
            int newSize = getConnectionWindowSize();
            assertEquals(initialSize + increment, newSize, "Connection window not increased correctly");

        } catch (Exception e) {
            fail("Failed to access processWindowUpdateFrame method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("readFully should handle complete data")
    public void testReadFullyWithCompleteData() throws IOException {
        // Create test data
        byte[] testData = { 1, 2, 3, 4, 5 };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);

        // Read exactly the right amount
        ByteBuffer buffer = ByteBuffer.allocate(testData.length);

        // Access the private method
        try {
            Method readFullyMethod = Http2Processor.class.getDeclaredMethod(
                    "readFully", InputStream.class, ByteBuffer.class);
            readFullyMethod.setAccessible(true);

            // Should succeed
            boolean result = (boolean) readFullyMethod.invoke(processor, inputStream, buffer);

            // Verify
            assertTrue(result, "readFully should return true for complete read");
            buffer.flip();
            assertEquals(testData.length, buffer.remaining(), "Buffer should contain all data");

            byte[] readData = new byte[buffer.remaining()];
            buffer.get(readData);

            // Compare arrays
            for (int i = 0; i < testData.length; i++) {
                assertEquals(testData[i], readData[i], "Data mismatch at index " + i);
            }

        } catch (Exception e) {
            fail("Failed to access readFully method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("readFully should handle EOF")
    public void testReadFullyWithEOF() throws IOException {
        // Create an empty input stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // Try to read from it
        ByteBuffer buffer = ByteBuffer.allocate(10);

        // Access the private method
        try {
            Method readFullyMethod = Http2Processor.class.getDeclaredMethod(
                    "readFully", InputStream.class, ByteBuffer.class);
            readFullyMethod.setAccessible(true);

            // Should return false indicating EOF
            boolean result = (boolean) readFullyMethod.invoke(processor, inputStream, buffer);
            assertFalse(result, "readFully should return false for EOF");

        } catch (Exception e) {
            fail("Failed to access readFully method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("createResponse should convert stream data to HTTP request")
    public void testCreateResponse() throws IOException {
        // Create a stream and add request data
        int streamId = 1;
        Http2Stream stream = connectionManager.createStream(streamId);

        // Add request headers
        stream.addRequestHeader(":method", "GET");
        stream.addRequestHeader(":path", "/test");
        stream.addRequestHeader(":scheme", "https");
        stream.addRequestHeader(":authority", "example.com");
        stream.addRequestHeader("user-agent", "test-client");

        // Initialize the processor with output
        processor.initialise(outputStream);

        // Call createResponse using reflection
        try {
            Method createResponseMethod = Http2Processor.class.getDeclaredMethod(
                    "createResponse", Http2Stream.class);
            createResponseMethod.setAccessible(true);

            HttpResponse response = (HttpResponse) createResponseMethod.invoke(processor, stream);

            // Verify response was created
            assertNotNull(response, "Response should not be null");
            assertEquals(streamId, response.getProperty("streamId"), "Stream ID not set in response");

        } catch (Exception e) {
            fail("Failed to access createResponse method: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("processHeadersFrame should properly decode HPACK headers")
    public void testProcessHeadersFrameHpackDecoding() throws Exception {
        // Create a real HPACK encoded header block
        // We'll use Twitter's HPACK encoder to create a valid header block
        ByteArrayOutputStream headerBlockStream = new ByteArrayOutputStream();
        com.twitter.hpack.Encoder encoder = new com.twitter.hpack.Encoder(4096);

        // Encode some sample headers
        encoder.encodeHeader(headerBlockStream, ":method".getBytes(), "GET".getBytes(), false);
        encoder.encodeHeader(headerBlockStream, ":path".getBytes(), "/test".getBytes(), false);
        encoder.encodeHeader(headerBlockStream, ":scheme".getBytes(), "https".getBytes(), false);
        encoder.encodeHeader(headerBlockStream, ":authority".getBytes(), "example.com".getBytes(), false);
        encoder.encodeHeader(headerBlockStream, "user-agent".getBytes(), "test-client".getBytes(), false);

        byte[] headerBlock = headerBlockStream.toByteArray();

        // Create a HEADERS frame with the encoded headers
        int streamId = 1;
        int flags = Http2Frame.FLAG_END_HEADERS | Http2Frame.FLAG_END_STREAM;
        ByteBuffer payload = ByteBuffer.wrap(headerBlock);

        // Initialize the processor with output stream
        processor.initialise(outputStream);

        // Use reflection to access the private method
        Method processHeadersFrameMethod = Http2Processor.class.getDeclaredMethod(
                "processHeadersFrame", int.class, int.class, ByteBuffer.class);
        processHeadersFrameMethod.setAccessible(true);

        // Call the method with our HPACK encoded headers
        HttpResponse response = (HttpResponse) processHeadersFrameMethod.invoke(
                processor, streamId, flags, payload);

        // Get the stream that was created
        Http2Stream stream = connectionManager.getStream(streamId);
        assertNotNull(stream, "A stream should have been created");

        // Verify the headers were decoded correctly
        assertEquals("GET", stream.getRequestHeader(":method"), "Method header was not decoded correctly");
        assertEquals("/test", stream.getRequestHeader(":path"), "Path header was not decoded correctly");
        assertEquals("https", stream.getRequestHeader(":scheme"), "Scheme header was not decoded correctly");
        assertEquals("example.com", stream.getRequestHeader(":authority"),
                "Authority header was not decoded correctly");
        assertEquals("test-client", stream.getRequestHeader("user-agent"),
                "User-Agent header was not decoded correctly");

        // Verify a response was created since we used END_STREAM flag
        assertNotNull(response, "A response should be generated with END_STREAM flag");
        assertEquals(streamId, response.getProperty("streamId"), "Response should have the stream ID set");
    }

    // Helper method to get the connection window size using reflection
    private int getConnectionWindowSize() throws IOException {
        try {
            java.lang.reflect.Field field = Http2ConnectionManager.class.getDeclaredField("connectionWindowSize");
            field.setAccessible(true);
            return (int) field.get(connectionManager);
        } catch (Exception e) {
            fail("Could not access connectionWindowSize field: " + e.getMessage());
            return 0; // Never reached
        }
    }
}
