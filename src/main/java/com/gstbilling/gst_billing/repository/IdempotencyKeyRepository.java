package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, Long> {

    Optional<IdempotencyKeyRecord> findByBusiness_IdAndIdempotencyKey(Long businessId, String idempotencyKey);

    @Transactional
    @Modifying
    @Query("DELETE FROM IdempotencyKeyRecord r WHERE r.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);

    @Transactional
    void deleteByBusiness_Id(Long businessId);
}
