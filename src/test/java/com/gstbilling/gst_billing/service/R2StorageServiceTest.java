package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.storage.R2StorageService;
import com.gstbilling.gst_billing.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2StorageServiceTest {

    private S3Client s3Client;
    private R2StorageService r2Storage;
    private static final String BUCKET = "test-r2-bucket";
    private static final String ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
    private static final String PUBLIC_URL = "https://cdn.example.com";

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        r2Storage = new R2StorageService(s3Client, BUCKET, ENDPOINT, PUBLIC_URL);
    }

    @Test
    @DisplayName("store() successfully uploads complete byte array with correct bucket, key, and content type")
    void testStoreSuccess() throws IOException {
        String key = "invoices/101/invoice-101.pdf";
        byte[] pdfContent = "%PDF-1.4 sample pdf binary data".getBytes(StandardCharsets.UTF_8);

        PutObjectResponse putResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putResponse);

        String resultUrl = r2Storage.store(key, pdfContent, "application/pdf");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET, capturedRequest.bucket());
        assertEquals("invoices/101/invoice-101.pdf", capturedRequest.key());
        assertEquals("application/pdf", capturedRequest.contentType());
        assertEquals(pdfContent.length, capturedRequest.contentLength());

        byte[] uploadedBytes = bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes();
        assertArrayEquals(pdfContent, uploadedBytes);

        assertEquals("https://cdn.example.com/invoices/101/invoice-101.pdf", resultUrl);
    }

    @Test
    @DisplayName("store() rejects empty content and null arguments explicitly")
    void testStoreRejectsEmptyOrNullContent() {
        assertThrows(IllegalArgumentException.class, () ->
                r2Storage.store("test.pdf", new byte[0], "application/pdf")
        );
        assertThrows(IllegalArgumentException.class, () ->
                r2Storage.store("test.pdf", null, "application/pdf")
        );
        assertThrows(IllegalArgumentException.class, () ->
                r2Storage.store(null, "some content".getBytes(), "text/plain")
        );
    }

    @Test
    @DisplayName("store() strictly enforces path traversal protection")
    void testStoreRejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                r2Storage.store("../secret.txt", "data".getBytes(), "text/plain")
        );
        assertThrows(IllegalArgumentException.class, () ->
                r2Storage.store("invoices/../../passwd", "data".getBytes(), "text/plain")
        );
    }

    @Test
    @DisplayName("store() propagates StorageException on S3 service or network error and never falls back")
    void testStorePropagatesStorageExceptionOnFailure() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("Internal R2 Error").build());

        StorageException ex = assertThrows(StorageException.class, () ->
                r2Storage.store("invoices/fail.pdf", "pdf bytes".getBytes(), "application/pdf")
        );
        assertTrue(ex.getMessage().contains("Failed to store object in Cloudflare R2"));
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("retrieve() returns exact uploaded bytes on success")
    void testRetrieveSuccess() {
        String key = "receipts/5/receipt-5.pdf";
        byte[] expected = "exact binary receipt content".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse getResponse = GetObjectResponse.builder().contentLength((long) expected.length).build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, expected);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] actual = r2Storage.retrieve(key);
        assertNotNull(actual);
        assertArrayEquals(expected, actual);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("receipts/5/receipt-5.pdf", captor.getValue().key());
    }

    @Test
    @DisplayName("retrieve() returns null when object is missing (NoSuchKeyException or 404)")
    void testRetrieveMissingReturnsNull() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Object not found").build());

        byte[] result = r2Storage.retrieve("invoices/missing.pdf");
        assertNull(result);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        byte[] result404 = r2Storage.retrieve("invoices/missing-404.pdf");
        assertNull(result404);
    }

    @Test
    @DisplayName("retrieve() throws StorageException on 403 Access Denied or network failure")
    void testRetrieveAccessDeniedThrowsStorageException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Access Denied").build());

        StorageException ex = assertThrows(StorageException.class, () ->
                r2Storage.retrieve("private/invoices/10.pdf")
        );
        assertTrue(ex.getMessage().contains("Access denied"));

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.create("Socket timeout"));

        assertThrows(StorageException.class, () ->
                r2Storage.retrieve("private/invoices/10.pdf")
        );
    }

    @Test
    @DisplayName("exists() returns true when object exists, false when missing, throws on error")
    void testExistsBehavior() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
        assertTrue(r2Storage.exists("invoices/101.pdf"));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());
        assertFalse(r2Storage.exists("invoices/999.pdf"));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not found").build());
        assertFalse(r2Storage.exists("invoices/404.pdf"));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());
        assertThrows(StorageException.class, () -> r2Storage.exists("invoices/forbidden.pdf"));
    }

    @Test
    @DisplayName("delete() deletes object, returns true on success, false when missing, throws on failure")
    void testDeleteBehavior() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());
        assertTrue(r2Storage.delete("invoices/to-delete.pdf"));

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());
        assertFalse(r2Storage.delete("invoices/already-gone.pdf"));

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("S3 failure").build());
        assertThrows(StorageException.class, () -> r2Storage.delete("invoices/fail.pdf"));
    }

    @Test
    @DisplayName("getUrl() returns public URL when configured, safe relative endpoint when unconfigured")
    void testUrlResolution() {
        R2StorageService r2WithPublic = new R2StorageService(s3Client, BUCKET, ENDPOINT, "https://assets.mybrand.com");
        assertEquals("https://assets.mybrand.com/logos/biz-1.png", r2WithPublic.getUrl("logos/biz-1.png"));

        R2StorageService r2WithoutPublic = new R2StorageService(s3Client, BUCKET, ENDPOINT, null);
        // Distinguishes internal storage keys from public URLs and prevents exposing private S3 API endpoints
        assertEquals("/uploads/invoices/1/101.pdf", r2WithoutPublic.getUrl("invoices/1/101.pdf"));
    }

    @Test
    @DisplayName("isHealthy() verifies bucket connectivity via headBucket without reading/writing objects")
    void testHealthCheck() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
        assertTrue(r2Storage.isHealthy());

        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Invalid credentials").build());
        assertFalse(r2Storage.isHealthy());
    }
}
