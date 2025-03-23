package com.app;

import com.twitter.hpack.Encoder;
import com.twitter.hpack.Decoder;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.OutputStream;

public class Http2ConnectionManager extends ConnectionManager {
    private final ConcurrentHashMap<Integer, Http2Stream> streams = new ConcurrentHashMap<>();

    private final Http2Settings localSettings = new Http2Settings();
    private final Http2Settings remoteSettings = new Http2Settings();

    private int connectionWindowSize = 65535;

    private int lastStreamId = 0;
    private boolean goAwaySent = false;

    private final Encoder encoder = new Encoder(remoteSettings.getHeaderTableSize());
    private final Decoder decoder = new Decoder(remoteSettings.getMaxHeaderListSize(),
            remoteSettings.getHeaderTableSize());

    public Http2Stream getStream(int streamId) {
        return streams.get(streamId);
    }

    public Http2Stream createStream(int streamId) {
        Http2Stream stream = new Http2Stream(streamId, this);
        streams.put(streamId, stream);
        return stream;
    }

    public void removeStream(int streamId) {
        streams.remove(streamId);
    }

    public Collection<Http2Stream> getAllStreams() {
        return streams.values();
    }

    public Http2Settings getLocalSettings() {
        return localSettings;
    }

    public void updateLocalSettings(Http2Settings settings) {
        localSettings.merge(settings);
    }

    public Http2Settings getRemoteSettings() {
        return remoteSettings;
    }

    public void updateRemoteSettings(Http2Settings settings) {
        remoteSettings.merge(settings);
    }

    public synchronized boolean consumeConnectionWindow(int size) {
        if (connectionWindowSize >= size) {
            connectionWindowSize -= size;
            return true;
        }
        return false;
    }

    public synchronized void increaseConnectionWindow(int increment) {
        connectionWindowSize += increment;
    }

    public synchronized int getNextStreamId() {
        lastStreamId += 2;
        return lastStreamId;
    }

    public void sendGoAway(int lastStreamId, int errorCode) {
        goAwaySent = true;
    }

    public boolean isGoAwaySent() {
        return goAwaySent;
    }

    public void sendFrame(Http2Frame frame, OutputStream output) throws IOException {
        ByteBuffer encodedFrame = frame.encode();

        byte[] frameBytes = new byte[encodedFrame.remaining()];
        encodedFrame.get(frameBytes);
        output.write(frameBytes);
        output.flush();

        if (frame instanceof SettingsFrame && !((SettingsFrame) frame).isAck()) {
            SettingsFrame settingsFrame = (SettingsFrame) frame;
            localSettings.merge(settingsFrame.getSettings());
        }

        if (frame instanceof GoAwayFrame) {
            goAwaySent = true;
        }
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }

}
