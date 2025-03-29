package com.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.twitter.hpack.Encoder;
import com.twitter.hpack.Decoder;

@ExtendWith(MockitoExtension.class)
public class Http2ConnectionManagerTest {
    private Http2ConnectionManager connectionManager;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setUp() {
        connectionManager = new Http2ConnectionManager();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testCreateAndGetStream() {
        int streamId = 1;
        Http2Stream stream = connectionManager.createStream(streamId);

        assertEquals(streamId, stream.getStreamId());
        assertSame(stream, connectionManager.getStream(streamId));
    }

    @Test
    public void testRemoveStream() {
        int streamId = 1;
        connectionManager.createStream(streamId);

        assertNotNull(connectionManager.getStream(streamId));

        connectionManager.removeStream(streamId);

        assertNull(connectionManager.getStream(streamId));
    }

    @Test
    public void testGetAllStreams() {
        connectionManager.createStream(1);
        connectionManager.createStream(3);
        connectionManager.createStream(5);

        Collection<Http2Stream> streams = connectionManager.getAllStreams();

        assertEquals(3, streams.size());

        assertTrue(streams.stream().anyMatch(s -> s.getStreamId() == 1));
        assertTrue(streams.stream().anyMatch(s -> s.getStreamId() == 3));
        assertTrue(streams.stream().anyMatch(s -> s.getStreamId() == 5));
    }

    @Test
    public void testGetNextStreamId() {
        int firstId = connectionManager.getNextStreamId();
        int secondId = connectionManager.getNextStreamId();
        int thirdId = connectionManager.getNextStreamId();

        assertEquals(firstId + 2, secondId);
        assertEquals(secondId + 2, thirdId);
    }

    @Test
    public void testLocalSettings() {
        Http2Settings newSettings = new Http2Settings();
        newSettings.setHeaderTableSize(4096);
        newSettings.setInitialWindowSize(65535);

        connectionManager.updateLocalSettings(newSettings);
        Http2Settings localSettings = connectionManager.getLocalSettings();

        assertEquals(4096, localSettings.getHeaderTableSize());
        assertEquals(65535, localSettings.getInitialWindowSize());
    }

    @Test
    public void testRemoteSettings() {
        Http2Settings newSettings = new Http2Settings();
        newSettings.setMaxConcurrentStreams(100);
        newSettings.setMaxFrameSize(16384);

        connectionManager.updateRemoteSettings(newSettings);

        Http2Settings remoteSettings = connectionManager.getRemoteSettings();

        assertEquals(100, remoteSettings.getMaxConcurrentStreams());
        assertEquals(16384, remoteSettings.getMaxFrameSize());
    }

    @Test
    public void testConnectionWindow() {
        connectionManager.consumeConnectionWindow(1000);
        connectionManager.increaseConnectionWindow(2000);

        boolean result = connectionManager.consumeConnectionWindow(1500);
        assertTrue(result);
        result = connectionManager.consumeConnectionWindow(100000);

        assertFalse(result);
    }

    @Test
    public void testGoAwayFlags() {
        assertFalse(connectionManager.isGoAwaySent());
        assertFalse(connectionManager.isGoAwayReceived());

        connectionManager.sendGoAway(0, Http2Frame.NO_ERROR);
        assertTrue(connectionManager.isGoAwaySent());

        connectionManager.markGoAwayReceived();
        assertTrue(connectionManager.isGoAwayReceived());
    }

    @Test
    public void testGetEncoder() {
        Encoder encoder = connectionManager.getEncoder();
        assertNotNull(encoder);
    }

    @Test
    public void testGetDecoder() {
        Decoder decoder = connectionManager.getDecoder();
        assertNotNull(decoder);
    }

    @Test
    public void testSendFrame() throws IOException {
        SettingsFrame settingsFrame = new SettingsFrame(false);
        connectionManager.sendFrame(settingsFrame, outputStream);

        assertTrue(outputStream.size() > 0);
    }

    @Test
    public void testSendFrameWithException() {
        SettingsFrame settingsFrame = new SettingsFrame(false);

        OutputStream badOutputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void write(byte[] b) throws IOException {
                throw new IOException("Test exception");
            }
        };

        Exception exception = assertThrows(IOException.class, () -> {
            connectionManager.sendFrame(settingsFrame, badOutputStream);
        });

        assertTrue(exception.getMessage().contains("Failed to send frame"));
    }

    // @Test
    // public void testSendSettingsFrameUpdatesLocalSettings() throws IOException {
    // Http2Settings initialSettings = connectionManager.getLocalSettings();
    // int initialHeaderTableSize = initialSettings.getHeaderTableSize();
    // System.out.println("Initial header table size: " + initialHeaderTableSize);
    //
    // Http2Settings settings = new Http2Settings();
    // settings.setHeaderTableSize(8192);
    // settings.setEnablePush(false); // Disable push
    // SettingsFrame settingsFrame = new SettingsFrame(settings);
    //
    // System.out.println("Frame header table size: " +
    // settingsFrame.getSettings().getHeaderTableSize());
    //
    // connectionManager.sendFrame(settingsFrame, outputStream);
    //
    // Http2Settings localSettings = connectionManager.getLocalSettings();
    // System.out.println("Local settings header table size after send: " +
    // localSettings.getHeaderTableSize());
    //
    // assertEquals(8192, localSettings.getHeaderTableSize());
    // assertEquals(false, localSettings.isPushEnabled());
    // }
}
