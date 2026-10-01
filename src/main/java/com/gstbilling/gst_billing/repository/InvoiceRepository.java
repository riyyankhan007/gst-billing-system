package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}