package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.security.TenantContext;
import com.gstbilling.gst_billing.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadControllerTest {

    @TempDir
    Path tempUploads;

    private StorageService storageService;
    private UploadController controller;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        controller = new UploadController(storageService, tempUploads.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getUploadedFile() retrieves asset from StorageService when present")
    void testGetUploadedFileFromStorageService() throws IOException {
        String filename = "business-1-logo-123.png";
        byte[] imageBytes = new byte[]{0x12, 0x34, 0x56};

        when(storageService.exists("logos/" + filename)).thenReturn(true);
        when(storageService.retrieve("logos/" + filename)).thenReturn(imageBytes);

        TenantContext.setTenantId(1L);
        ResponseEntity<Resource> response = controller.getUploadedFile(filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(imageBytes, response.getBody().getInputStream().readAllBytes());
    }

    @Test
    @DisplayName("getUploadedFile() falls back to local disk for legacy files")
    void testGetUploadedFileFallbackToLegacyLocalDisk() throws IOException {
        String filename = "business-1-signature-legacy.jpg";
        Path localFile = tempUploads.resolve(filename);
        Files.write(localFile, "legacy image data".getBytes(StandardCharsets.UTF_8));

        when(storageService.exists(anyString())).thenReturn(false);

        TenantContext.setTenantId(1L);
        ResponseEntity<Resource> response = controller.getUploadedFile(filename);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("getUploadedFile() rejects path traversal attempts with 400 Bad Request")
    void testPathTraversalRejected() {
        assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("../secret.png")
        );
        assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("folder/secret.png")
        );
    }

    @Test
    @DisplayName("getUploadedFile() enforces tenant boundary with 403 Forbidden")
    void testTenantIsolationEnforced() {
        TenantContext.setTenantId(2L); // Different tenant

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("business-1-logo-999.png")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getUploadedFile() returns 404 Not Found when file does not exist anywhere")
    void testNotFoundReturned() {
        when(storageService.exists(anyString())).thenReturn(false);

        TenantContext.setTenantId(1L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("business-1-nonexistent.png")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
