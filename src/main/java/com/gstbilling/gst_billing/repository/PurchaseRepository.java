package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByBusiness_IdOrderByPurchaseDateDescIdDesc(Long businessId);
    Optional<Purchase> findByIdAndBusiness_Id(Long id, Long businessId);
    List<Purchase> findBySupplier_IdAndBusiness_IdOrderByPurchaseDateDesc(Long supplierId, Long businessId);

    @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM Purchase p WHERE p.business.id = :businessId")
    BigDecimal sumTotalPurchasesByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(p.totalTax), 0) FROM Purchase p WHERE p.business.id = :businessId")
    BigDecimal sumTotalInputTaxByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM Purchase p WHERE p.business.id = :businessId")
    BigDecimal sumPaidPurchasesByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(p.balanceAmount), 0) FROM Purchase p WHERE p.business.id = :businessId")
    BigDecimal sumOutstandingPurchasesByBusinessId(@Param("businessId") Long businessId);
}
