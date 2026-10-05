package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByBusiness_IdOrderByCreatedAtDesc(Long businessId);
    List<StockMovement> findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(Long productId, Long businessId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByBusiness_Id(Long businessId);
}
