package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByBusinessId(Long businessId);
    Optional<Invoice> findByIdAndBusinessId(Long id, Long businessId);
}
