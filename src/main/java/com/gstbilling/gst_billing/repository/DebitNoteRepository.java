package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    List<DebitNote> findByBusiness_IdOrderByNoteDateDescIdDesc(Long businessId);
    Optional<DebitNote> findByIdAndBusiness_Id(Long id, Long businessId);
    boolean existsByBusiness_IdAndDebitNoteNumberIgnoreCase(Long businessId, String debitNoteNumber);
}
