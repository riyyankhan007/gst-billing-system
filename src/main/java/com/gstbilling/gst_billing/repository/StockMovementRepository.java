package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT sm FROM StockMovement sm " +
           "LEFT JOIN FETCH sm.product " +
           "LEFT JOIN FETCH sm.warehouse " +
           "LEFT JOIN FETCH sm.targetWarehouse " +
           "WHERE sm.business.id = :businessId " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByBusiness_IdOrderByCreatedAtDesc(@Param("businessId") Long businessId);

    @Query("SELECT sm FROM StockMovement sm " +
           "LEFT JOIN FETCH sm.product " +
           "LEFT JOIN FETCH sm.warehouse " +
           "LEFT JOIN FETCH sm.targetWarehouse " +
           "WHERE sm.warehouse.id = :warehouseId AND sm.business.id = :businessId " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByWarehouse_IdAndBusiness_IdOrderByCreatedAtDesc(@Param("warehouseId") Long warehouseId, @Param("businessId") Long businessId);

    @Query("SELECT sm FROM StockMovement sm " +
           "LEFT JOIN FETCH sm.product " +
           "LEFT JOIN FETCH sm.warehouse " +
           "LEFT JOIN FETCH sm.targetWarehouse " +
           "WHERE sm.product.id = :productId AND sm.business.id = :businessId " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByProduct_IdAndBusiness_IdOrderByCreatedAtDesc(@Param("productId") Long productId, @Param("businessId") Long businessId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByBusiness_Id(Long businessId);
}
