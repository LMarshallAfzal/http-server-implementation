package com.app;

import java.nio.ByteBuffer;

/**
 * SETTINGS frame (type=0x4) contains configuration parameters.
 */
class SettingsFrame extends Http2Frame {
    public SettingsFrame(int streamId, int flags, ByteBuffer payload) {
        super(streamId, flags, payload);
    }

    public SettingsFrame(boolean ack) {
        super(0, ack ? FLAG_ACK : 0);
    }

    public SettingsFrame(Http2Settings settings) {
        super(0, 0);

        // Each setting is 6 bytes: 2 bytes identifier, 4 bytes value
        ByteBuffer buffer = ByteBuffer.allocate(6 * 6); // Maximum 6 settings

        // Add each setting that differs from default
        addSetting(buffer, Http2Settings.SETTINGS_HEADER_TABLE_SIZE, settings.getHeaderTableSize());
        addSetting(buffer, Http2Settings.SETTINGS_ENABLE_PUSH, settings.isPushEnabled() ? 1 : 0);
        addSetting(buffer, Http2Settings.SETTINGS_MAX_CONCURRENT_STREAMS, settings.getMaxConcurrentStreams());
        addSetting(buffer, Http2Settings.SETTINGS_INITIAL_WINDOW_SIZE, settings.getInitialWindowSize());
        addSetting(buffer, Http2Settings.SETTINGS_MAX_FRAME_SIZE, settings.getMaxFrameSize());
        addSetting(buffer, Http2Settings.SETTINGS_MAX_HEADER_LIST_SIZE, settings.getMaxHeaderListSize());

        buffer.flip();
        this.payload = buffer;
    }

    private void addSetting(ByteBuffer buffer, int id, int value) {
        buffer.putShort((short) id);
        buffer.putInt(value);
    }

    @Override
    public int getType() {
        return TYPE_SETTINGS;
    }

    public boolean isAck() {
        return hasFlag(FLAG_ACK);
    }

    public Http2Settings getSettings() {
        Http2Settings settings = new Http2Settings();

        ByteBuffer buffer = payload.duplicate();
        buffer.flip();

        while (buffer.remaining() >= 6) {
            int id = buffer.getShort() & 0xFFFF;
            int value = buffer.getInt();

            switch (id) {
                case Http2Settings.SETTINGS_HEADER_TABLE_SIZE:
                    settings.setHeaderTableSize(value);
                    break;
                case Http2Settings.SETTINGS_ENABLE_PUSH:
                    settings.setEnablePush(value == 1);
                    break;
                case Http2Settings.SETTINGS_MAX_CONCURRENT_STREAMS:
                    settings.setMaxConcurrentStreams(value);
                    break;
                case Http2Settings.SETTINGS_INITIAL_WINDOW_SIZE:
                    settings.setInitialWindowSize(value);
                    break;
                case Http2Settings.SETTINGS_MAX_FRAME_SIZE:
                    settings.setMaxFrameSize(value);
                    break;
                case Http2Settings.SETTINGS_MAX_HEADER_LIST_SIZE:
                    settings.setMaxHeaderListSize(value);
                    break;
            }
        }

        return settings;
    }
}
