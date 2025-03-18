package com.app;

/**
 * The Http2Settings class encapsulates the HTTP/2 protocol settings parameters
 *
 * <p>
 * This class provides storage and access for the standard HTTP/2 settings as
 * defined in
 * <a href="https://tools.ietf.org/html/rfc7540#section-6.5.2>RFC 7540 Section
 * 6.5.2</a>.
 * It initialises all settings to their default values according to the HTTP/2
 * specification,
 * and provides methods to get and set each setting.
 * </p>
 */
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

    /**
     * Constructs a new Http2Settings instance with defult values as specified by
     * the HTTP/2 protocol
     * 
     * <ul>
     * <li>Initial Window Size: 65535 bytes</li>
     * <li>Header Table Size: 4096 bytes</li>
     * <li>Enable Push: true</li>
     * <li>Max Concurrent Stream: Integer.MAX_VALUE</li>
     * <li>Max Frame Size: 16384 bytes</li>
     * <li>Max Header List Size: 8192 bytes</li>
     * </ul>
     */
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
