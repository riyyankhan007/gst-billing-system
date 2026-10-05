package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.business.id = :businessId AND s.financialYear = :financialYear AND s.docType = :docType")
    Optional<InvoiceSequence> findByBusinessAndFyAndDocTypeForUpdate(
            @Param("businessId") Long businessId,
            @Param("financialYear") String financialYear,
            @Param("docType") String docType
    );

    Optional<InvoiceSequence> findByBusiness_IdAndFinancialYearAndDocType(
            Long businessId,
            String financialYear,
            String docType
    );

    @org.springframework.transaction.annotation.Transactional
    void deleteByBusiness_Id(Long businessId);
}
