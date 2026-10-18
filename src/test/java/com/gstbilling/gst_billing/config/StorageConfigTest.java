package com.gstbilling.gst_billing.config;

import com.gstbilling.gst_billing.storage.LocalStorageService;
import com.gstbilling.gst_billing.storage.R2StorageService;
import com.gstbilling.gst_billing.storage.StorageConfigurationException;
import com.gstbilling.gst_billing.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigTest {

    private final StorageConfig storageConfig = new StorageConfig();

    @Test
    @DisplayName("Explicit R2 mode fails application startup when credentials or endpoint are missing")
    void testExplicitR2MissingVariablesFailsFast() {
        // Missing all
        StorageConfigurationException ex = assertThrows(StorageConfigurationException.class, () ->
                storageConfig.storageService("r2", "", "", "", "", null, "uploads")
        );
        assertTrue(ex.getMessage().contains("R2_ACCESS_KEY_ID"));
        assertTrue(ex.getMessage().contains("R2_ENDPOINT"));
        assertTrue(ex.getMessage().contains("R2_BUCKET_NAME"));

        // Partially missing
        StorageConfigurationException exPartial = assertThrows(StorageConfigurationException.class, () ->
                storageConfig.storageService("r2", "my-key", "my-secret", "", "my-bucket", null, "uploads")
        );
        assertTrue(exPartial.getMessage().contains("R2_ENDPOINT"));
        assertFalse(exPartial.getMessage().contains("R2_ACCESS_KEY_ID"));
    }

    @Test
    @DisplayName("Explicit R2 mode initializes R2StorageService when all 4 required credentials are provided")
    void testExplicitR2AllConfiguredInitializesR2() {
        StorageService service = storageConfig.storageService(
                "r2",
                "test-access-key",
                "test-secret-key",
                "https://test.r2.cloudflarestorage.com",
                "test-bucket",
                "https://pub-cdn.example.com",
                "uploads"
        );
        assertNotNull(service);
        assertInstanceOf(R2StorageService.class, service);
        assertEquals("R2", service.getProviderName());
    }

    @Test
    @DisplayName("Explicit local mode initializes LocalStorageService independently")
    void testExplicitLocalModeInitializesLocalStorage() {
        StorageService service = storageConfig.storageService(
                "local",
                "",
                "",
                "",
                "",
                "",
                "target/test-uploads"
        );
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
        assertEquals("LOCAL", service.getProviderName());
    }

    @Test
    @DisplayName("Auto mode selects Cloudflare R2 when all credentials are set")
    void testAutoModeSelectsR2WhenConfigured() {
        StorageService service = storageConfig.storageService(
                "auto",
                "key123",
                "sec456",
                "https://auto.r2.cloudflarestorage.com",
                "auto-bucket",
                null,
                "uploads"
        );
        assertNotNull(service);
        assertInstanceOf(R2StorageService.class, service);
        assertEquals("R2", service.getProviderName());
    }

    @Test
    @DisplayName("Auto mode selects Local storage when no R2 credentials are set")
    void testAutoModeSelectsLocalWhenEmpty() {
        StorageService service = storageConfig.storageService(
                "auto",
                "",
                "",
                "",
                "",
                null,
                "target/test-uploads"
        );
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
        assertEquals("LOCAL", service.getProviderName());
    }

    @Test
    @DisplayName("Auto mode falls back to Local storage with warning when credentials are only partially set")
    void testAutoModeSelectsLocalWhenPartiallyConfigured() {
        StorageService service = storageConfig.storageService(
                "auto",
                "only-access-key-provided",
                "",
                "",
                "",
                null,
                "target/test-uploads"
        );
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
        assertEquals("LOCAL", service.getProviderName());
    }

    @Test
    @DisplayName("Unsupported storage provider throws StorageConfigurationException")
    void testUnsupportedProviderThrowsException() {
        assertThrows(StorageConfigurationException.class, () ->
                storageConfig.storageService("s3_direct", "", "", "", "", null, "uploads")
        );
    }
}
