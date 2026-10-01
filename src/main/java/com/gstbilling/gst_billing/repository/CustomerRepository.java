package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByBusinessId(Long businessId);
    Optional<Customer> findByIdAndBusinessId(Long id, Long businessId);
}
