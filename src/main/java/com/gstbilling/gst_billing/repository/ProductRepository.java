package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}