package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.controller.UploadController;
import com.gstbilling.gst_billing.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FileAuthorizationSecurityTest {

    private StorageService mockStorageService;
    private UploadController controller;

    @BeforeEach
    void setUp() {
        mockStorageService = Mockito.mock(StorageService.class);
        controller = new UploadController(mockStorageService, "uploads");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Path traversal attempts with '..' or slashes are immediately rejected with HTTP 400")
    void testPathTraversalRejection() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("../etc/passwd")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("folder/secret.key")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    @Test
    @DisplayName("Anonymous unauthenticated access to private signatures or invoice PDFs is rejected with HTTP 401")
    void testAnonymousAccessToPrivateFilesRejected() {
        TenantContext.clear(); // Anonymous caller

        ResponseStatusException exSignature = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("business-101-signature-abc.png")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exSignature.getStatusCode());

        ResponseStatusException exPdf = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("business-101-invoice-INV001.pdf")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exPdf.getStatusCode());
    }

    @Test
    @DisplayName("Cross-tenant access to private signatures or files is rejected with HTTP 403 Forbidden")
    void testCrossTenantAccessToPrivateFilesRejected() {
        // Authenticated as Tenant 202
        TenantContext.setTenantId(202L);

        // Attempting to access Tenant 101's private signature
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.getUploadedFile("business-101-signature-xyz.png")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("belongs to another business"));
    }

    @Test
    @DisplayName("Matching tenant can successfully access their own private signature")
    void testMatchingTenantCanAccessOwnPrivateFile() {
        // Authenticated as Tenant 101
        TenantContext.setTenantId(101L);

        byte[] fakePng = new byte[]{1, 2, 3, 4};
        when(mockStorageService.exists("signatures/business-101-signature-valid.png")).thenReturn(true);
        when(mockStorageService.retrieve("signatures/business-101-signature-valid.png")).thenReturn(fakePng);

        ResponseEntity<?> response = controller.getUploadedFile("business-101-signature-valid.png");
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getCacheControl().contains("private"));
    }
}
