package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.WarehouseStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    Optional<WarehouseStock> findByWarehouse_IdAndProduct_Id(Long warehouseId, Long productId);

    default Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId) {
        return findByWarehouse_IdAndProduct_Id(warehouseId, productId);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.warehouse.id = :warehouseId AND ws.product.id = :productId")
    Optional<WarehouseStock> findForUpdateByWarehouseIdAndProductId(@Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

    List<WarehouseStock> findByWarehouse_Id(Long warehouseId);

    List<WarehouseStock> findByBusiness_Id(Long businessId);

    List<WarehouseStock> findByProduct_Id(Long productId);

    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.business.id = :businessId AND ws.product.id = :productId")
    List<WarehouseStock> findByBusinessIdAndProductId(@Param("businessId") Long businessId, @Param("productId") Long productId);
}
