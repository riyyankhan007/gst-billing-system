package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.HsnSacCode;
import com.gstbilling.gst_billing.entity.HsnSacType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HsnSacCodeRepository extends JpaRepository<HsnSacCode, Long> {

    Optional<HsnSacCode> findByCodeAndType(String code, HsnSacType type);

    Optional<HsnSacCode> findByCodeAndTypeAndActiveTrue(String code, HsnSacType type);

    boolean existsByCodeAndType(String code, HsnSacType type);

    @Query("SELECT h FROM HsnSacCode h WHERE h.active = true " +
           "AND (:type IS NULL OR h.type = :type) " +
           "AND (:q IS NULL OR :q = '' OR LOWER(h.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(h.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(h.code) = LOWER(:q) THEN 0 " +
           "     WHEN LOWER(h.code) LIKE LOWER(CONCAT(:q, '%')) THEN 1 " +
           "     ELSE 2 END, h.code ASC")
    Page<HsnSacCode> searchCatalogue(@Param("q") String q, @Param("type") HsnSacType type, Pageable pageable);
}
