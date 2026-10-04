package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBusiness_Id(Long businessId);

    List<Invoice> findByBusiness_IdOrderByCreatedAtDesc(Long businessId);

    List<Invoice> findByBusiness_IdOrderByInvoiceDateDescIdDesc(Long businessId);

    Optional<Invoice> findByIdAndBusiness_Id(Long id, Long businessId);

    List<Invoice> findByCustomer_IdAndBusiness_Id(Long customerId, Long businessId);

    List<Invoice> findByCustomer_IdAndBusiness_IdOrderByCreatedAtDesc(Long customerId, Long businessId);

    List<Invoice> findByBusiness_IdAndInvoiceDateBetween(Long businessId, LocalDate startDate, LocalDate endDate);

    boolean existsByBusiness_IdAndInvoiceNumberIgnoreCase(Long businessId, String invoiceNumber);

    List<Invoice> findByBusiness_IdAndStatus(Long businessId, String status);

    @Query("SELECT i FROM Invoice i WHERE i.business.id = :businessId " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:customerId IS NULL OR i.customer.id = :customerId) " +
           "ORDER BY i.invoiceDate DESC, i.id DESC")
    List<Invoice> searchInvoices(
            @Param("businessId") Long businessId,
            @Param("status") String status,
            @Param("customerId") Long customerId
    );

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumGrandTotalByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.invoiceDate = :date AND i.status != 'CANCELLED'")
    BigDecimal sumGrandTotalByBusinessIdAndDate(@Param("businessId") Long businessId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.invoiceDate BETWEEN :startDate AND :endDate AND i.status != 'CANCELLED'")
    BigDecimal sumGrandTotalByBusinessIdAndDateBetween(@Param("businessId") Long businessId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(i.taxableAmount), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumTaxableAmountByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(i.cgst), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumCgstByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(i.sgst), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumSgstByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(i.igst), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumIgstByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(i.totalTax), 0) FROM Invoice i WHERE i.business.id = :businessId AND i.status != 'CANCELLED'")
    BigDecimal sumTotalTaxByBusinessId(@Param("businessId") Long businessId);
}