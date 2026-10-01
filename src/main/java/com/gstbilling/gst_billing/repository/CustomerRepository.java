package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}