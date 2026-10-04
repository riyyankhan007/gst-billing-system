package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByBusinessId(Long businessId);
    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);
    List<Product> findByBusinessIdAndActiveTrue(Long businessId);
    List<Product> findByBusinessIdAndProductType(Long businessId, String productType);

    @Query("SELECT p FROM Product p WHERE p.business.id = :businessId AND p.productType = 'PRODUCT' AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts(@Param("businessId") Long businessId);
}
