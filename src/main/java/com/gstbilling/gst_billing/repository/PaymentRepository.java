package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBusiness_IdOrderByPaymentDateDesc(Long businessId);
    List<Payment> findByInvoice_IdAndBusiness_IdOrderByPaymentDateDesc(Long invoiceId, Long businessId);
    List<Payment> findByCustomer_IdAndBusiness_IdOrderByPaymentDateDesc(Long customerId, Long businessId);
    Optional<Payment> findByIdAndBusiness_Id(Long id, Long businessId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.business.id = :businessId")
    BigDecimal sumAmountByInvoiceIdAndBusinessId(@Param("invoiceId") Long invoiceId, @Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.business.id = :businessId")
    BigDecimal sumTotalPaymentsByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.business.id = :businessId AND p.paymentDate = :date")
    BigDecimal sumPaymentsByBusinessIdAndDate(@Param("businessId") Long businessId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.business.id = :businessId AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPaymentsByBusinessIdAndDateBetween(@Param("businessId") Long businessId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
