package com.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class Http2SettingsTest {

    @Test
    public void testDefaultInitialization() {
        Http2Settings settings = new Http2Settings();

        assertEquals(65535, settings.getInitialWindowSize());
        assertEquals(4096, settings.getHeaderTableSize());
        assertTrue(settings.isPushEnabled());
        assertEquals(Integer.MAX_VALUE, settings.getMaxConcurrentStreams());
        assertEquals(16384, settings.getMaxFrameSize());
        assertEquals(8192, settings.getMaxHeaderListSize());
    }

    @Test
    public void testSetAndGetInitialWindowSize() {
        Http2Settings settings = new Http2Settings();
        int newSize = 32768;

        settings.setInitialWindowSize(newSize);
        assertEquals(newSize, settings.getInitialWindowSize());
    }

    @Test
    public void testSetAndGetHeaderTableSize() {
        Http2Settings settings = new Http2Settings();
        int newSize = 8192;

        settings.setHeaderTableSize(newSize);
        assertEquals(newSize, settings.getHeaderTableSize());
    }

    @Test
    public void testSetAndGetEnablePush() {
        Http2Settings settings = new Http2Settings();

        settings.setEnablePush(false);
        assertFalse(settings.isPushEnabled());

        settings.setEnablePush(true);
        assertTrue(settings.isPushEnabled());
    }

    @Test
    public void testSetAndGetMaxConcurrentStreams() {
        Http2Settings settings = new Http2Settings();
        int newValue = 100;

        settings.setMaxConcurrentStreams(newValue);
        assertEquals(newValue, settings.getMaxConcurrentStreams());
    }

    @Test
    public void testSetAndGetMaxFrameSize() {
        Http2Settings settings = new Http2Settings();
        int newSize = 24576;

        settings.setMaxFrameSize(newSize);
        assertEquals(newSize, settings.getMaxFrameSize());
    }

    @Test
    public void testSetAndGetMaxHeaderListSize() {
        Http2Settings settings = new Http2Settings();
        int newSize = 16384;

        settings.setMaxHeaderListSize(newSize);
        assertEquals(newSize, settings.getMaxHeaderListSize());
    }

    @Test
    public void testMergeWithNullSettings() {
        Http2Settings settings = new Http2Settings();
        int originalHeaderTableSize = settings.getHeaderTableSize();

        settings.merge(null);

        assertEquals(originalHeaderTableSize, settings.getHeaderTableSize());
    }

    @Test
    public void testMergeWithNonNullSettings() {
        Http2Settings settings1 = new Http2Settings();
        Http2Settings settings2 = new Http2Settings();

        settings2.setHeaderTableSize(8192);
        settings2.setEnablePush(false);
        settings2.setMaxConcurrentStreams(100);
        settings2.setInitialWindowSize(32768);
        settings2.setMaxFrameSize(24576);
        settings2.setMaxHeaderListSize(16384);

        settings1.merge(settings2);

        assertEquals(8192, settings1.getHeaderTableSize());
        assertFalse(settings1.isPushEnabled());
        assertEquals(100, settings1.getMaxConcurrentStreams());
        assertEquals(32768, settings1.getInitialWindowSize());
        assertEquals(24576, settings1.getMaxFrameSize());
        assertEquals(16384, settings1.getMaxHeaderListSize());
    }

    @Test
    public void testFromSingleSettingHeaderTableSize() {
        int value = 8192;
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_HEADER_TABLE_SIZE, value);

        assertEquals(value, settings.getHeaderTableSize());

        assertEquals(65535, settings.getInitialWindowSize());
        assertTrue(settings.isPushEnabled());
        assertEquals(Integer.MAX_VALUE, settings.getMaxConcurrentStreams());
        assertEquals(16384, settings.getMaxFrameSize());
        assertEquals(8192, settings.getMaxHeaderListSize());
    }

    @Test
    public void testFromSingleSettingEnablePush() {
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_ENABLE_PUSH, 0);
        assertFalse(settings.isPushEnabled());

        settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_ENABLE_PUSH, 1);
        assertTrue(settings.isPushEnabled());
    }

    @Test
    public void testFromSingleSettingMaxConcurrentStreams() {
        int value = 100;
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_MAX_CONCURRENT_STREAMS, value);
        assertEquals(value, settings.getMaxConcurrentStreams());
    }

    @Test
    public void testFromSingleSettingInitialWindowSize() {
        int value = 32768;
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_INITIAL_WINDOW_SIZE, value);
        assertEquals(value, settings.getInitialWindowSize());
    }

    @Test
    public void testFromSingleSettingMaxFrameSize() {
        int value = 24576;
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_MAX_FRAME_SIZE, value);
        assertEquals(value, settings.getMaxFrameSize());
    }

    @Test
    public void testFromSingleSettingMaxHeaderListSize() {
        int value = 16384;
        Http2Settings settings = Http2Settings.fromSingleSetting(Http2Settings.SETTINGS_MAX_HEADER_LIST_SIZE, value);
        assertEquals(value, settings.getMaxHeaderListSize());
    }

    @Test
    public void testFromSingleSettingWithInvalidId() {
        Http2Settings settings = Http2Settings.fromSingleSetting(0x7, 100);

        assertEquals(65535, settings.getInitialWindowSize());
        assertEquals(4096, settings.getHeaderTableSize());
        assertTrue(settings.isPushEnabled());
        assertEquals(Integer.MAX_VALUE, settings.getMaxConcurrentStreams());
        assertEquals(16384, settings.getMaxFrameSize());
        assertEquals(8192, settings.getMaxHeaderListSize());
    }

    @Test
    public void testIdentityAfterMultipleGetters() {
        Http2Settings settings = new Http2Settings();

        settings.setHeaderTableSize(8192);

        int value1 = settings.getHeaderTableSize();
        int value2 = settings.getHeaderTableSize();
        int value3 = settings.getHeaderTableSize();

        assertEquals(8192, value1);
        assertEquals(value1, value2);
        assertEquals(value2, value3);
    }
}
