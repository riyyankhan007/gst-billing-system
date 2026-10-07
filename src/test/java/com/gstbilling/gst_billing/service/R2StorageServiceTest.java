package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.storage.R2StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class R2StorageServiceTest {

    @Test
    @DisplayName("R2StorageService gracefully falls back to local storage when credentials are empty")
    void testR2FallbackToLocal() {
        R2StorageService r2 = new R2StorageService("", "", "", "", "", "uploads");

        String path = "test/doc-" + UUID.randomUUID() + ".txt";
        byte[] content = "Cloudflare R2 Fallback Content".getBytes(StandardCharsets.UTF_8);

        // Store
        String storedUrl = r2.store(path, content, "text/plain");
        assertNotNull(storedUrl);
        assertTrue(r2.exists(path));

        // Retrieve
        byte[] retrieved = r2.retrieve(path);
        assertNotNull(retrieved);
        assertArrayEquals(content, retrieved);

        // Delete
        assertTrue(r2.delete(path));
        assertFalse(r2.exists(path));
    }

    @Test
    @DisplayName("R2StorageService enforces path traversal protection")
    void testPathTraversalProtection() {
        R2StorageService r2 = new R2StorageService("", "", "", "", "", "uploads");

        assertThrows(IllegalArgumentException.class, () ->
                r2.store("../traversal.txt", "content".getBytes(), "text/plain")
        );
        assertThrows(IllegalArgumentException.class, () ->
                r2.retrieve("../../etc/passwd")
        );
    }

    @Test
    @DisplayName("R2StorageService resolves public URL correctly")
    void testR2UrlResolution() {
        R2StorageService r2WithPublic = new R2StorageService(
                "key", "secret", "https://acc.r2.cloudflarestorage.com", "my-bucket", "https://pub.cdn.com", "uploads"
        );
        assertEquals("https://pub.cdn.com/invoices/1.pdf", r2WithPublic.getUrl("invoices/1.pdf"));

        R2StorageService r2EndpointUrl = new R2StorageService(
                "key", "secret", "https://acc.r2.cloudflarestorage.com", "gst-billing-production", "", "uploads"
        );
        assertEquals("https://acc.r2.cloudflarestorage.com/gst-billing-production/invoices/1.pdf", r2EndpointUrl.getUrl("invoices/1.pdf"));
    }
}
