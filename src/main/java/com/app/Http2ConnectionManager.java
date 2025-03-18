package com.app;

import com.twitter.hpack.Encoder;
import com.twitter.hpack.Decoder;

import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Http2ConnectionManager extends ConnectionManager {
    private final ConcurrentHashMap<Integer, Http2Stream> streams = new ConcurrentHashMap<>();

    private final Http2Settings localSettings = new Http2Settings();
    private final Http2Settings remoteSettings = new Http2Settings();

    private int connectionWindowSize = 65535;

    private int lastStreamId = 0;
    private boolean goAwaySent = false;

    private final Encoder encoder = new Encoder(4096);
    private final Decoder decoder = new Decoder();

    public Http2ConnectionManager(Socket socket) {
        super(socket);
    }

    public Http2Stream getStream(int streamId) {
        return streams.get(streamId);
    }

    public Http2Stream createStream(int streamId) {
        Http2Stream stream = new Http2Stream(streamId);
        streams.put(streamId, stream);
        return stream;
    }

    public void removeStream(intStreamId) {
        streams.put(streamId, stream);
        return stream;
    }

    public Collection<Http2Stream> getAllStreams() {
        return streams.values();
    }

    public void updateLocalSettings(Http2Setting settings) {
        localSettings.merge(settings);
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

    public void sendFrame(Http2Frame frame) throws IOException {
        // Implementation to encode and send a frame
    }

    public HpackEncoder getEncoder() {
        return encoder;
    }

    public HpackDecoder getDecoder() {
        return decoder;
    }

    @Override
    public void closeConnection(String connectionId) {
        if (!goAwaySent) {
            sendGoAway(lastStreamId, 0);
        }
        super.closeConnection(connectionId);




    
}
