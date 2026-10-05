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

    @Query("SELECT i.invoiceDate, " +
           "COALESCE(SUM(i.grandTotal), 0), " +
           "COALESCE(SUM(i.totalTax), 0), " +
           "COALESCE(SUM(i.cgst), 0), " +
           "COALESCE(SUM(i.sgst), 0), " +
           "COALESCE(SUM(i.igst), 0), " +
           "COUNT(i) " +
           "FROM Invoice i " +
           "WHERE i.business.id = :businessId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.status != 'CANCELLED' " +
           "GROUP BY i.invoiceDate " +
           "ORDER BY i.invoiceDate ASC")
    List<Object[]> getSalesAndTaxTrendByDateBetween(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT c.id, c.name, " +
           "COUNT(i), " +
           "COALESCE(SUM(i.grandTotal), 0), " +
           "COALESCE(SUM(i.balanceAmount), 0) " +
           "FROM Invoice i JOIN i.customer c " +
           "WHERE i.business.id = :businessId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.status != 'CANCELLED' " +
           "GROUP BY c.id, c.name " +
           "ORDER BY SUM(i.grandTotal) DESC")
    List<Object[]> getTopCustomersByDateBetween(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT it.productId, it.productName, " +
           "COALESCE(SUM(it.quantity), 0), " +
           "COALESCE(SUM(it.totalAmount), 0) " +
           "FROM InvoiceItem it JOIN it.invoice i " +
           "WHERE i.business.id = :businessId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.status != 'CANCELLED' " +
           "GROUP BY it.productId, it.productName " +
           "ORDER BY SUM(it.totalAmount) DESC")
    List<Object[]> getTopProductsByDateBetween(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.grandTotal), 0), COALESCE(SUM(i.paidAmount), 0), COALESCE(SUM(i.balanceAmount), 0) " +
           "FROM Invoice i " +
           "WHERE i.business.id = :businessId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY i.status")
    List<Object[]> getStatusBreakdownByDateBetween(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(i), COALESCE(SUM(i.balanceAmount), 0) FROM Invoice i " +
           "WHERE i.business.id = :businessId " +
           "AND i.dueDate < :today " +
           "AND i.balanceAmount > 0 " +
           "AND i.status != 'CANCELLED'")
    List<Object[]> getOverdueSummary(
            @Param("businessId") Long businessId,
            @Param("today") LocalDate today
    );

    @Query("SELECT COALESCE(SUM(i.totalTax), 0), COALESCE(SUM(i.cgst), 0), COALESCE(SUM(i.sgst), 0), COALESCE(SUM(i.igst), 0), COALESCE(SUM(i.balanceAmount), 0) " +
           "FROM Invoice i " +
           "WHERE i.business.id = :businessId " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND i.status != 'CANCELLED'")
    List<Object[]> getPeriodTaxAndBalanceSummary(
            @Param("businessId") Long businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}