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
    private boolean goAwayReceived = false;

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

    public void markGoAwayReceived() {
        goAwayReceived = true;
    }

    public boolean isGoAwayReceived() {
        return goAwayReceived;
    }

    public void sendFrame(Http2Frame frame, OutputStream output) throws IOException {
        try {
            dumpFrame("SENDING", frame);

            ByteBuffer encodedFrame = frame.encode();

            byte[] frameBytes = new byte[encodedFrame.remaining()];
            encodedFrame.get(frameBytes);

            System.out.println("SENDING FRAME: type=" + frame.getType() +
                    ", stream=" + frame.getStreamId() +
                    ", flags=" + frame.getFlags() +
                    ", length=" + frameBytes.length);

            output.write(frameBytes);
            output.flush();

            if (frame instanceof SettingsFrame && !((SettingsFrame) frame).isAck()) {
                SettingsFrame settingsFrame = (SettingsFrame) frame;
                localSettings.merge(settingsFrame.getSettings());
            }

            if (frame instanceof GoAwayFrame) {
                goAwaySent = true;
            }
        } catch (Exception e) {
            System.err.println("Error sending frame: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to send frame", e);
        }
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public static void dumpFrame(String prefix, Http2Frame frame) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(" Frame Type: ").append(frameTypeToString(frame.getType()));
        sb.append(", Stream ID: ").append(frame.getStreamId());
        sb.append(", Flags: ").append(Integer.toHexString(frame.getFlags())).append(" (");

        // Decode flags
        if ((frame.getFlags() & Http2Frame.FLAG_END_STREAM) != 0)
            sb.append("END_STREAM ");
        if ((frame.getFlags() & Http2Frame.FLAG_END_HEADERS) != 0)
            sb.append("END_HEADERS ");
        if ((frame.getFlags() & Http2Frame.FLAG_ACK) != 0)
            sb.append("ACK ");
        if ((frame.getFlags() & Http2Frame.FLAG_PADDED) != 0)
            sb.append("PADDED ");
        if ((frame.getFlags() & Http2Frame.FLAG_PRIORITY) != 0)
            sb.append("PRIORITY ");
        sb.append(")");

        // Specific frame type details
        if (frame instanceof HeadersFrame) {
            sb.append(" [HEADERS] EndStream: ").append(((HeadersFrame) frame).isEndStream());
            sb.append(", EndHeaders: ").append(((HeadersFrame) frame).isEndHeaders());
        } else if (frame instanceof DataFrame) {
            sb.append(" [DATA] EndStream: ").append(((DataFrame) frame).isEndStream());
        } else if (frame instanceof SettingsFrame) {
            sb.append(" [SETTINGS] IsAck: ").append(((SettingsFrame) frame).isAck());
        }

        // Payload info
        ByteBuffer payload = frame.getPayload();
        sb.append(", Payload size: ").append(payload.remaining()).append(" bytes");

        // For HEADERS frames, try to decode the headers (simplified)
        if (frame instanceof HeadersFrame && payload.remaining() > 0) {
            sb.append("\nHeader block (hex): ");
            byte[] data = new byte[Math.min(payload.remaining(), 50)];
            int originalPosition = payload.position();
            payload.get(data);
            payload.position(originalPosition); // Reset position

            for (byte b : data) {
                sb.append(String.format("%02X ", b & 0xFF));
            }
            if (payload.remaining() > 50) {
                sb.append("...");
            }
        }

        System.out.println(sb.toString());
    }

    private static String frameTypeToString(int type) {
        switch (type) {
            case Http2Frame.TYPE_DATA:
                return "DATA";
            case Http2Frame.TYPE_HEADERS:
                return "HEADERS";
            case Http2Frame.TYPE_PRIORITY:
                return "PRIORITY";
            case Http2Frame.TYPE_RST_STREAM:
                return "RST_STREAM";
            case Http2Frame.TYPE_SETTINGS:
                return "SETTINGS";
            case Http2Frame.TYPE_PUSH_PROMISE:
                return "PUSH_PROMISE";
            case Http2Frame.TYPE_PING:
                return "PING";
            case Http2Frame.TYPE_GOAWAY:
                return "GOAWAY";
            case Http2Frame.TYPE_WINDOW_UPDATE:
                return "WINDOW_UPDATE";
            case Http2Frame.TYPE_CONTINUATION:
                return "CONTINUATION";
            default:
                return "UNKNOWN";
        }
    }

}
