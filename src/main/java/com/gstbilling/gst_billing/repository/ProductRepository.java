package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByBusinessId(Long businessId);
    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);
}
