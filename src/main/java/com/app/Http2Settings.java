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

    /**
     * Sets the initial window size for stream-level flow control.
     *
     * @param The initial window size in bytes
     */
    public void setInitialWindowSize(int size) {
        initialWindowSize = size;
    }

    /**
     * Gets the initial window for stream-level flow control.
     *
     * @return The initial window size in bytes
     */
    public int getInitialWindowSize() {
        return initialWindowSize;
    }

    /**
     * Sets the maximum size for the header compression table.
     *
     * @param size The maximum header table size in bytes
     */
    public void setHeaderTableSize(int size) {
        headerTableSize = size;
    }

    /**
     * Gets the maximum size of the header compression table.
     *
     * @return The maximum header table size in bytes
     */
    public int getHeaderTableSize() {
        return headerTableSize;
    }

    /**
     * Sets the maximum number of concurrent streams allowed.
     *
     * @return The maximum number of concurrent streams
     */
    public void setMaxConcurrentStreams(int concurrentStreams) {
        maxConcurrentStreams = concurrentStreams;
    }

    /**
     * Gets the maximum number of concurrent streams allowed
     * 
     * @return The maximum number of concurrent streams
     */
    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    /**
     * Sets the maximum frame size allowed
     *
     * @param frameSize The maximum frame size in bytes
     */
    public void setMaxFrameSize(int frameSize) {
        maxFrameSize = frameSize;
    }

    /**
     * Sets the maximum frame size allowed.
     *
     * @param frameSize The maximum frame size in bytes
     */
    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    /**
     * Sets the maximum header list size allowed.
     *
     * @param headerListSize The maximum header list size in bytes
     */
    public void setMaxHeaderListSize(int headerListSize) {
        maxHeaderListSize = headerListSize;
    }

    /**
     * Gets the maximum header list size allowed.
     *
     * @param headerListSize The maximum header list size in bytes
     */
    public int getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    /**
     * Sets whether server push is enabled
     *
     * @param push true to enable server push, false to disable
     */
    public void setEnablePush(boolean push) {
        enablePush = push;
    }

    /**
     * Checks if server push is enabled.
     *
     * @return true if server push is enabled, false otherwise
     */
    public boolean isPushEnabled() {
        return enablePush;
    }

}
