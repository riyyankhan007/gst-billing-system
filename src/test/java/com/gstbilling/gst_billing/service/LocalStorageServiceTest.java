package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.storage.LocalStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LocalStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("store() saves bytes and retrieve() returns exact bytes")
    void testStoreAndRetrieve() {
        String path = "documents/test-" + UUID.randomUUID() + ".txt";
        byte[] content = "Hello Local Storage".getBytes(StandardCharsets.UTF_8);

        String url = storage.store(path, content, "text/plain");
        assertNotNull(url);
        assertTrue(storage.exists(path));

        byte[] retrieved = storage.retrieve(path);
        assertNotNull(retrieved);
        assertArrayEquals(content, retrieved);

        assertTrue(storage.delete(path));
        assertFalse(storage.exists(path));
    }

    @Test
    @DisplayName("store() rejects empty content and path traversal")
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                storage.store("test.txt", new byte[0], "text/plain")
        );
        assertThrows(IllegalArgumentException.class, () ->
                storage.store("../secret.txt", "content".getBytes(), "text/plain")
        );
    }

    @Test
    @DisplayName("isHealthy() returns true when base directory is writable")
    void testHealthCheck() {
        assertTrue(storage.isHealthy());
        assertEquals("LOCAL", storage.getProviderName());
    }
}
