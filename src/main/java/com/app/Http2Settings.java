package com.app;

public class Http2Settings {

    public static final int SETTINGS_HEADER_TABLE_SIZE = 0x1;
    public static final int SETTINGS_ENABLE_PUSH = 0x2;
    public static final int SETTINGS_MAX_CONCURRENT_STREAMS = 0x3;
    public static final int SETTINGS_INITIAL_WINDOW_SIZE = 0x4;
    public static final int SETTINGS_MAX_FRAME_SIZE = 0x5;
    public static final int SETTINGS_MAX_HEADER_LIST_SIZE = 0x6;

    private int headerTableSize;
    private boolean enablePush;
    private int maxConcurrentStreams;
    private int initialWindowSize;
    private int maxFrameSize;
    private int maxHeaderListSize;

    public Http2Settings() {
        initialWindowSize = 65535;
        headerTableSize = 4096;
        enablePush = true;
        maxConcurrentStreams = Integer.MAX_VALUE;
        maxFrameSize = 16384;
        maxHeaderListSize = 8192;
    }

    public void setInitialWindowSize(int size) {
        initialWindowSize = size;
    }

    public int getInitialWindowSize() {
        return initialWindowSize;
    }

    public void setHeaderTableSize(int size) {
        headerTableSize = size;
    }

    public int getHeaderTableSize() {
        return headerTableSize;
    }

    public void setMaxConcurrentStreams(int concurrentStreams) {
        maxConcurrentStreams = concurrentStreams;
    }

    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxFrameSize(int frameSize) {
        maxFrameSize = frameSize;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxHeaderListSize(int headerListSize) {
        maxHeaderListSize = headerListSize;
    }

    public int getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    public void setEnablePush(boolean push) {
        enablePush = push;
    }

    public boolean isPushEnabled() {
        return enablePush;
    }

}
