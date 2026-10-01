package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {
}