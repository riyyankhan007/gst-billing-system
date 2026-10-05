package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.IdempotencyKeyRecord;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    public enum IdempotencyStatus {
        CACHED_RESPONSE,
        IN_PROGRESS,
        PAYLOAD_MISMATCH,
        NEW_KEY
    }

    public record IdempotencyCheckResult(
            IdempotencyStatus status,
            IdempotencyKeyRecord record
    ) {}

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final BusinessRepository businessRepository;

    public IdempotencyService(
            IdempotencyKeyRepository idempotencyKeyRepository,
            BusinessRepository businessRepository
    ) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.businessRepository = businessRepository;
    }

    public String computeRequestHash(String method, String uri, byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(method.getBytes(StandardCharsets.UTF_8));
            md.update((byte) ':');
            md.update(uri.getBytes(StandardCharsets.UTF_8));
            md.update((byte) ':');
            if (body != null && body.length > 0) {
                md.update(body);
            }
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyCheckResult checkOrLockKey(Long businessId, String key, String requestHash) {
        Optional<IdempotencyKeyRecord> opt = idempotencyKeyRepository.findByBusiness_IdAndIdempotencyKey(businessId, key);

        if (opt.isPresent()) {
            IdempotencyKeyRecord existing = opt.get();
            if (existing.isExpired()) {
                idempotencyKeyRepository.delete(existing);
                idempotencyKeyRepository.flush();
            } else {
                if (!existing.getRequestHash().equals(requestHash)) {
                    return new IdempotencyCheckResult(IdempotencyStatus.PAYLOAD_MISMATCH, existing);
                }
                if (existing.isInFlight()) {
                    return new IdempotencyCheckResult(IdempotencyStatus.IN_PROGRESS, existing);
                }
                return new IdempotencyCheckResult(IdempotencyStatus.CACHED_RESPONSE, existing);
            }
        }

        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) {
            return new IdempotencyCheckResult(IdempotencyStatus.NEW_KEY, null);
        }

        IdempotencyKeyRecord newRecord = new IdempotencyKeyRecord(business, key, requestHash, 24);
        IdempotencyKeyRecord saved = idempotencyKeyRepository.saveAndFlush(newRecord);
        return new IdempotencyCheckResult(IdempotencyStatus.NEW_KEY, saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordResponse(Long businessId, String key, int statusCode, String responseBody, String contentType) {
        idempotencyKeyRepository.findByBusiness_IdAndIdempotencyKey(businessId, key).ifPresent(record -> {
            record.setResponseStatus(statusCode);
            record.setResponseBody(responseBody);
            if (contentType != null) {
                record.setResponseContentType(contentType);
            }
            idempotencyKeyRepository.saveAndFlush(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseKey(Long businessId, String key) {
        idempotencyKeyRepository.findByBusiness_IdAndIdempotencyKey(businessId, key)
                .ifPresent(record -> {
                    if (record.isInFlight()) {
                        idempotencyKeyRepository.delete(record);
                        idempotencyKeyRepository.flush();
                    }
                });
    }
}
