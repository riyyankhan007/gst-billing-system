package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    List<CreditNote> findByBusiness_IdOrderByNoteDateDescIdDesc(Long businessId);
    Optional<CreditNote> findByIdAndBusiness_Id(Long id, Long businessId);
    List<CreditNote> findByInvoice_IdAndBusiness_Id(Long invoiceId, Long businessId);
    boolean existsByBusiness_IdAndCreditNoteNumberIgnoreCase(Long businessId, String creditNoteNumber);
}
