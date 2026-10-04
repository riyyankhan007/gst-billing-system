package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Business;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    boolean existsByGstinIgnoreCaseAndIdNot(String gstin, Long id);
    boolean existsByGstinIgnoreCase(String gstin);
    Optional<Business> findByGstinIgnoreCase(String gstin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Business b WHERE b.id = :id")
    Optional<Business> findByIdForUpdate(@Param("id") Long id);
}