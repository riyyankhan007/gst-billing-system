package com.gstbilling.gst_billing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderKeepAliveServiceTest {

    @Test
    @DisplayName("RenderKeepAliveService normalizes external URL to /api/ping")
    void testRenderKeepAliveUrlNormalization() {
        RenderKeepAliveService serviceWithTrailingSlash =
                new RenderKeepAliveService("https://gst-billing.onrender.com/");
        assertEquals("https://gst-billing.onrender.com/api/ping", serviceWithTrailingSlash.getPingUrl());

        RenderKeepAliveService serviceWithoutTrailingSlash =
                new RenderKeepAliveService("https://gst-billing.onrender.com");
        assertEquals("https://gst-billing.onrender.com/api/ping", serviceWithoutTrailingSlash.getPingUrl());

        RenderKeepAliveService serviceWithApiSuffix =
                new RenderKeepAliveService("https://gst-billing.onrender.com/api");
        assertEquals("https://gst-billing.onrender.com/api/ping", serviceWithApiSuffix.getPingUrl());
    }

    @Test
    @DisplayName("RenderKeepAliveService stays idle when URL is empty or null")
    void testRenderKeepAliveIdleWhenUrlEmpty() {
        RenderKeepAliveService serviceEmpty = new RenderKeepAliveService("");
        assertNull(serviceEmpty.getPingUrl());
        assertDoesNotThrow(serviceEmpty::pingSelf);

        RenderKeepAliveService serviceNull = new RenderKeepAliveService(null);
        assertNull(serviceNull.getPingUrl());
        assertDoesNotThrow(serviceNull::pingSelf);
    }
}
