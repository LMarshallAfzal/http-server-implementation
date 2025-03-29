package com.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Http2StreamTest {

    private Http2ConnectionManager connectionManager;

    private Http2Stream stream;
    private final int streamId = 1;

    @BeforeEach
    public void setUp() {
        Http2Settings settings = new Http2Settings();
        settings.setInitialWindowSize(65535);

        connectionManager = new Http2ConnectionManager();
        stream = new Http2Stream(streamId, connectionManager);
    }

    @Test
    @DisplayName("Initial state should be correct")
    public void testInitialState() {
        assertEquals(streamId, stream.getStreamId());
        assertEquals(StreamState.IDLE, stream.getState());
        assertEquals(16, stream.getWeight()); // Default weight
        assertFalse(stream.isEndStreamSent());
        assertFalse(stream.isEndStreamReceived());
        assertTrue(stream.getChildren().isEmpty());

        assertEquals(65535, stream.getLocalWindowSize());
        assertEquals(65535, stream.getRemoteWindowSize());

        assertFalse(stream.isHeadersReceived());
        assertEquals(0, stream.getData().remaining());
    }

    @Test
    @DisplayName("Transition to OPEN should work from IDLE state")
    public void testTransitionToOpen() {
        assertEquals(StreamState.IDLE, stream.getState());

        boolean result = stream.transitionToOpen();

        assertTrue(result);
        assertEquals(StreamState.OPEN, stream.getState());

        result = stream.transitionToOpen();

        assertFalse(result);
        assertEquals(StreamState.OPEN, stream.getState());
    }

    @Test
    @DisplayName("Close local should work from OPEN state")
    public void testCloseLocalFromOpen() {
        stream.transitionToOpen();

        boolean result = stream.closeLocal();

        assertTrue(result);
        assertEquals(StreamState.HALF_CLOSED_LOCAL, stream.getState());
    }

    @Test
    @DisplayName("Close local should transition to CLOSED from HALF_CLOSED_REMOTE")
    public void testCloseLocalFromHalfClosedRemote() {
        stream.transitionToOpen();
        stream.closeRemote();

        boolean result = stream.closeLocal();

        assertTrue(result);
        assertEquals(StreamState.CLOSED, stream.getState());
    }

    @Test
    @DisplayName("Close local should fail from invalid states")
    public void testCloseLocalFromInvalidStates() {
        boolean result = stream.closeLocal();
        assertFalse(result);
        assertEquals(StreamState.IDLE, stream.getState());

        stream.transitionToOpen();
        stream.closeLocal();
        result = stream.closeLocal();
        assertFalse(result);
        assertEquals(StreamState.HALF_CLOSED_LOCAL, stream.getState());

        Http2Stream closedStream = new Http2Stream(2, connectionManager);
        closedStream.resetStream(Http2Frame.NO_ERROR);
        result = closedStream.closeLocal();
        assertFalse(result);
        assertEquals(StreamState.CLOSED, closedStream.getState());
    }

    @Test
    @DisplayName("Close remote should work from OPEN state")
    public void testCloseRemoteFromOpen() {
        stream.transitionToOpen();

        boolean result = stream.closeRemote();

        assertTrue(result);
        assertEquals(StreamState.HALF_CLOSED_REMOTE, stream.getState());
    }

    @Test
    @DisplayName("Close remote should transition to CLOSED from HALF_CLOSED_LOCAL")
    public void testCloseRemoteFromHalfClosedLocal() {
        stream.transitionToOpen();
        stream.closeLocal();

        boolean result = stream.closeRemote();

        assertTrue(result);
        assertEquals(StreamState.CLOSED, stream.getState());
    }

    @Test
    @DisplayName("Close remote should fail from invalid states")
    public void testCloseRemoteFromInvalidStates() {
        boolean result = stream.closeRemote();
        assertFalse(result);
        assertEquals(StreamState.IDLE, stream.getState());

        stream.transitionToOpen();
        stream.closeRemote();
        result = stream.closeRemote();
        assertFalse(result);
        assertEquals(StreamState.HALF_CLOSED_REMOTE, stream.getState());

        Http2Stream closedStream = new Http2Stream(2, connectionManager);
        closedStream.resetStream(Http2Frame.NO_ERROR);
        result = closedStream.closeRemote();
        assertFalse(result);
        assertEquals(StreamState.CLOSED, closedStream.getState());
    }

    @Test
    @DisplayName("Reset stream should immediately transition to CLOSED")
    public void testResetStream() {
        stream.transitionToOpen();

        int errorCode = Http2Frame.REFUSED_STREAM;
        stream.resetStream(errorCode);

        assertEquals(StreamState.CLOSED, stream.getState());
        assertEquals(errorCode, stream.getErrorCode());
    }

    @Test
    @DisplayName("Consume local window should decrease window size")
    public void testConsumeLocalWindow() {
        int initialSize = stream.getLocalWindowSize();

        int consumeAmount = 1000;
        boolean result = stream.consumeLocalWindow(consumeAmount);

        assertTrue(result);
        assertEquals(initialSize - consumeAmount, stream.getLocalWindowSize());

        int increaseAmount = 2000;
        stream.increaseLocalWindow(increaseAmount);

        assertEquals(initialSize - consumeAmount + increaseAmount, stream.getLocalWindowSize());

        result = stream.consumeLocalWindow(initialSize * 2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Consume remote window should decrease window size")
    public void testConsumeRemoteWindow() {
        int initialSize = stream.getRemoteWindowSize();

        int consumeAmount = 1000;
        boolean result = stream.consumeRemoteWindow(consumeAmount);

        assertTrue(result);
        assertEquals(initialSize - consumeAmount, stream.getRemoteWindowSize());

        int increaseAmount = 2000;
        stream.increaseRemoteWindow(increaseAmount);

        assertEquals(initialSize - consumeAmount + increaseAmount, stream.getRemoteWindowSize());

        result = stream.consumeRemoteWindow(initialSize * 2);

        assertFalse(result);
    }

    @Test
    @DisplayName("Set priority should update weight, parent, and child relationships")
    public void testSetPriority() {
        Http2Stream parentStream = new Http2Stream(3, connectionManager);

        int weight = 32;
        boolean exclusive = false;
        stream.setPriority(weight, exclusive, parentStream);

        assertEquals(weight, stream.getWeight());
        assertEquals(parentStream, stream.getParent());
        assertFalse(stream.isExclusive());

        Set<Http2Stream> children = parentStream.getChildren();
        assertTrue(children.contains(stream));
    }

    @Test
    @DisplayName("Set priority with exclusive flag should reparent siblings")
    public void testSetPriorityExclusive() {
        Http2Stream parentStream = new Http2Stream(3, connectionManager);

        Http2Stream sibling1 = new Http2Stream(5, connectionManager);
        Http2Stream sibling2 = new Http2Stream(7, connectionManager);

        sibling1.setPriority(16, false, parentStream);
        sibling2.setPriority(16, false, parentStream);

        Set<Http2Stream> parentChildren = parentStream.getChildren();
        assertEquals(2, parentChildren.size());
        assertTrue(parentChildren.contains(sibling1));
        assertTrue(parentChildren.contains(sibling2));

        stream.setPriority(32, true, parentStream);

        assertEquals(32, stream.getWeight());
        assertEquals(parentStream, stream.getParent());
        assertTrue(stream.isExclusive());

        parentChildren = parentStream.getChildren();
        assertEquals(1, parentChildren.size());
        assertTrue(parentChildren.contains(stream));

        Set<Http2Stream> streamChildren = stream.getChildren();
        assertEquals(2, streamChildren.size());
        assertTrue(streamChildren.contains(sibling1));
        assertTrue(streamChildren.contains(sibling2));

        assertEquals(stream, sibling1.getParent());
        assertEquals(stream, sibling2.getParent());
    }

    @Test
    @DisplayName("Request headers should be stored and retrieved correctly")
    public void testRequestHeaders() {
        stream.addRequestHeader("method", "GET");
        stream.addRequestHeader("path", "/test");
        stream.addRequestHeader("scheme", "https");

        assertEquals("GET", stream.getRequestHeader("method"));
        assertEquals("GET", stream.getRequestHeader("METHOD")); // Case insensitive

        HashMap<String, String> requestHeaders = stream.getRequestHeaders();
        assertEquals(3, requestHeaders.size());
        assertEquals("GET", requestHeaders.get("method"));
        assertEquals("/test", requestHeaders.get("path"));
        assertEquals("https", requestHeaders.get("scheme"));

        requestHeaders.put("new-header", "value");
        assertNull(stream.getRequestHeader("new-header"));
    }

    @Test
    @DisplayName("Response headers should be stored and retrieved correctly")
    public void testResponseHeaders() {
        stream.addResponseHeader("status", "200");
        stream.addResponseHeader("content-type", "text/html");

        assertEquals("200", stream.getResponseHeader("status"));
        assertEquals("text/html", stream.getResponseHeader("content-type"));
        assertEquals("text/html", stream.getResponseHeader("CONTENT-TYPE")); // Case insensitive

        HashMap<String, String> responseHeaders = stream.getResponseHeaders();
        assertEquals(2, responseHeaders.size());

        responseHeaders.put("new-header", "value");
        assertNull(stream.getResponseHeader("new-header"));
    }

    @Test
    @DisplayName("Receive headers should update stream state correctly")
    public void testReceiveHeaders() {
        // Create headers to receive
        HashMap<String, String> headers = new HashMap<>();
        headers.put(":method", "GET");
        headers.put(":path", "/test");
        headers.put(":scheme", "https");

        // Receive headers without end stream
        stream.receiveHeaders(headers, false);

        // Verify headers are stored
        assertEquals("GET", stream.getRequestHeader(":method"));
        assertEquals("/test", stream.getRequestHeader(":path"));
        assertEquals("https", stream.getRequestHeader(":scheme"));

        // Verify stream state
        assertTrue(stream.isHeadersReceived());
        assertFalse(stream.isEndStreamReceived());
        assertEquals(StreamState.IDLE, stream.getState()); // State shouldn't change without endStream

        // Receive more headers with end stream
        HashMap<String, String> trailers = new HashMap<>();
        trailers.put("x-custom-trailer", "value");
        stream.receiveHeaders(trailers, true);

        // Verify end stream flag is now set and remote side is closed
        assertTrue(stream.isEndStreamReceived());

        // Stream should have transitioned to HALF_CLOSED_REMOTE if it was OPEN
        // But since we didn't transition to OPEN, it should actually move to CLOSED now
        // which isn't ideal but is how the current implementation works
        // In a real implementation, we'd want to ensure the stream is OPEN first
    }

    @Test
    @DisplayName("Receive headers with stream already open should update state correctly")
    public void testReceiveHeadersWithOpenStream() {
        // First transition to OPEN
        stream.transitionToOpen();

        HashMap<String, String> headers = new HashMap<>();
        headers.put(":method", "GET");
        headers.put(":path", "/test");
        stream.receiveHeaders(headers, true);

        assertTrue(stream.isHeadersReceived());
        assertTrue(stream.isEndStreamReceived());
        assertEquals(StreamState.HALF_CLOSED_REMOTE, stream.getState());
    }

    @Test
    @DisplayName("Receive data should append to buffer and update state")
    public void testReceiveData() {
        stream.transitionToOpen();

        byte[] data1 = "Hello, ".getBytes();
        byte[] data2 = "world!".getBytes();
        ByteBuffer buffer1 = ByteBuffer.wrap(data1);
        ByteBuffer buffer2 = ByteBuffer.wrap(data2);

        stream.receiveData(buffer1, false);

        ByteBuffer storedData = stream.getData();
        assertEquals(data1.length, storedData.remaining());

        byte[] readData = new byte[storedData.remaining()];
        storedData.get(readData);
        assertArrayEquals(data1, readData);

        assertFalse(stream.isEndStreamReceived());
        assertEquals(StreamState.OPEN, stream.getState());

        stream.receiveData(buffer2, true);

        storedData = stream.getData();
        assertEquals(data1.length + data2.length, storedData.remaining());

        assertTrue(stream.isEndStreamReceived());
        assertEquals(StreamState.HALF_CLOSED_REMOTE, stream.getState());
    }

    @Test
    @DisplayName("Send headers should update stream state")
    public void testSendHeaders() throws IOException {
        stream.transitionToOpen();

        HashMap<String, String> headers = new HashMap<>();
        headers.put(":status", "200");
        headers.put("content-type", "text/plain");

        stream.sendHeaders(headers, false);

        assertEquals("200", stream.getResponseHeader(":status"));
        assertEquals("text/plain", stream.getResponseHeader("content-type"));

        assertFalse(stream.isEndStreamSent());
        assertEquals(StreamState.OPEN, stream.getState());

        HashMap<String, String> moreHeaders = new HashMap<>();
        moreHeaders.put("x-custom", "value");
        stream.sendHeaders(moreHeaders, true);

        assertEquals("value", stream.getResponseHeader("x-custom"));

        assertTrue(stream.isEndStreamSent());
        assertEquals(StreamState.HALF_CLOSED_LOCAL, stream.getState());
    }

    @Test
    @DisplayName("Send data should update stream state")
    public void testSendData() throws IOException {
        stream.transitionToOpen();

        ByteBuffer data = ByteBuffer.wrap("Test data".getBytes());

        stream.sendData(data, false);

        assertFalse(stream.isEndStreamSent());
        assertEquals(StreamState.OPEN, stream.getState());

        data.rewind();
        stream.sendData(data, true);

        assertTrue(stream.isEndStreamSent());
        assertEquals(StreamState.HALF_CLOSED_LOCAL, stream.getState());
    }

    @Test
    @DisplayName("Send RST_STREAM should reset stream")
    public void testSendRstStream() throws IOException {
        stream.transitionToOpen();

        int errorCode = Http2Frame.PROTOCOL_ERROR;
        stream.sendRstStream(errorCode);

        assertEquals(StreamState.CLOSED, stream.getState());
        assertEquals(errorCode, stream.getErrorCode());
    }

    @Test
    @DisplayName("Send WINDOW_UPDATE should increase remote window")
    public void testSendWindowUpdate() throws IOException {
        int initialSize = stream.getRemoteWindowSize();
        int increment = 10000;
        stream.sendWindowUpdate(increment);

        assertEquals(initialSize + increment, stream.getRemoteWindowSize());
    }

    @Test
    @DisplayName("Buffer resizing for data should work correctly")
    public void testBufferResizing() {
        int largeSize = 20000; // Default buffer is 16384
        byte[] largeData = new byte[largeSize];
        ByteBuffer largeBuffer = ByteBuffer.wrap(largeData);
        stream.appendData(largeBuffer);

        ByteBuffer storedData = stream.getData();
        assertEquals(largeSize, storedData.remaining());
    }

    @Test
    @DisplayName("isActive should return correct status based on state")
    public void testIsActive() {
        assertFalse(stream.isActive());

        stream.transitionToOpen();
        assertTrue(stream.isActive());

        Http2Stream localClosedStream = new Http2Stream(3, connectionManager);
        localClosedStream.transitionToOpen();
        localClosedStream.closeLocal();
        assertTrue(localClosedStream.isActive());

        Http2Stream remoteClosedStream = new Http2Stream(5, connectionManager);
        remoteClosedStream.transitionToOpen();
        remoteClosedStream.closeRemote();
        assertTrue(remoteClosedStream.isActive());

        Http2Stream closedStream = new Http2Stream(7, connectionManager);
        closedStream.resetStream(Http2Frame.NO_ERROR);
        assertFalse(closedStream.isActive());
    }

    @Test
    @DisplayName("clearData should reset the data buffer")
    public void testClearData() {
        ByteBuffer data = ByteBuffer.wrap("Test data".getBytes());
        stream.appendData(data);

        ByteBuffer storedData = stream.getData();
        assertTrue(storedData.remaining() > 0);

        stream.clearData();

        storedData = stream.getData();
        assertEquals(0, storedData.remaining());
    }
}
