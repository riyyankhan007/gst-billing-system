package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.IdempotencyKeyRecord;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdempotencyServiceTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private BusinessRepository businessRepository;
    @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;
    @Autowired private IdempotencyService idempotencyService;

    private Business business;
    private String runId;

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().substring(0, 6);
        business = new Business();
        business.setName("Idempotency Business " + runId);
        business.setInvoicePrefix("ID" + runId.substring(0, 2).toUpperCase());
        business = businessRepository.save(business);
    }

    @AfterEach
    void tearDown() {
        if (business != null && business.getId() != null) {
            try { idempotencyKeyRepository.deleteByBusiness_Id(business.getId()); } catch (Exception ignored) {}
            try { businessRepository.deleteById(business.getId()); } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Idempotency lock and cache replay flow: NEW_KEY -> IN_PROGRESS -> CACHED_RESPONSE")
    void testIdempotencyLifecycle() {
        String key = "idemp-key-" + UUID.randomUUID();
        String method = "POST";
        String uri = "/api/invoices";
        byte[] body = "{\"amount\":1000}".getBytes(StandardCharsets.UTF_8);
        String hash = idempotencyService.computeRequestHash(method, uri, body);

        // 1. Initial request -> NEW_KEY
        IdempotencyService.IdempotencyCheckResult firstCheck =
                idempotencyService.checkOrLockKey(business.getId(), key, hash);
        assertEquals(IdempotencyService.IdempotencyStatus.NEW_KEY, firstCheck.status());
        assertNotNull(firstCheck.record());
        assertTrue(firstCheck.record().isInFlight());

        // 2. Concurrent/duplicate in-flight request -> IN_PROGRESS
        IdempotencyService.IdempotencyCheckResult secondCheck =
                idempotencyService.checkOrLockKey(business.getId(), key, hash);
        assertEquals(IdempotencyService.IdempotencyStatus.IN_PROGRESS, secondCheck.status());

        // 3. Downstream completes successfully, record response
        idempotencyService.recordResponse(business.getId(), key, 201, "{\"id\":101,\"status\":\"CREATED\"}", "application/json");

        // 4. Repeated retry with same key and payload -> CACHED_RESPONSE with saved body
        IdempotencyService.IdempotencyCheckResult thirdCheck =
                idempotencyService.checkOrLockKey(business.getId(), key, hash);
        assertEquals(IdempotencyService.IdempotencyStatus.CACHED_RESPONSE, thirdCheck.status());
        IdempotencyKeyRecord record = thirdCheck.record();
        assertEquals(201, record.getResponseStatus());
        assertEquals("{\"id\":101,\"status\":\"CREATED\"}", record.getResponseBody());
    }

    @Test
    @DisplayName("Payload mismatch detection: Re-using same key with different payload returns PAYLOAD_MISMATCH")
    void testPayloadMismatchDetection() {
        String key = "idemp-key-" + UUID.randomUUID();
        String hash1 = idempotencyService.computeRequestHash("POST", "/api/invoices", "{\"item\":\"A\"}".getBytes(StandardCharsets.UTF_8));
        String hash2 = idempotencyService.computeRequestHash("POST", "/api/invoices", "{\"item\":\"B\"}".getBytes(StandardCharsets.UTF_8));

        // First lock with hash1
        idempotencyService.checkOrLockKey(business.getId(), key, hash1);
        idempotencyService.recordResponse(business.getId(), key, 200, "{\"ok\":true}", "application/json");

        // Attempting to use same key with hash2
        IdempotencyService.IdempotencyCheckResult mismatchCheck =
                idempotencyService.checkOrLockKey(business.getId(), key, hash2);
        assertEquals(IdempotencyService.IdempotencyStatus.PAYLOAD_MISMATCH, mismatchCheck.status());
    }

    @Test
    @DisplayName("Key release allows safe retry after downstream error")
    void testKeyReleaseOnFailure() {
        String key = "idemp-key-" + UUID.randomUUID();
        String hash = idempotencyService.computeRequestHash("POST", "/api/payments", "{\"payment\":500}".getBytes(StandardCharsets.UTF_8));

        // 1. Initial lock
        IdempotencyService.IdempotencyCheckResult check = idempotencyService.checkOrLockKey(business.getId(), key, hash);
        assertEquals(IdempotencyService.IdempotencyStatus.NEW_KEY, check.status());

        // 2. Downstream throws exception -> releaseKey is called
        idempotencyService.releaseKey(business.getId(), key);

        // 3. Subsequent retry succeeds as NEW_KEY
        IdempotencyService.IdempotencyCheckResult retryCheck = idempotencyService.checkOrLockKey(business.getId(), key, hash);
        assertEquals(IdempotencyService.IdempotencyStatus.NEW_KEY, retryCheck.status());
    }
}
