package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
}