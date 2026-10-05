package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.EWayBill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EWayBillRepository extends JpaRepository<EWayBill, Long> {

    List<EWayBill> findByBusiness_IdOrderByCreatedAtDesc(Long businessId);

    List<EWayBill> findByInvoice_IdAndBusiness_IdOrderByCreatedAtDesc(Long invoiceId, Long businessId);

    Optional<EWayBill> findByIdAndBusiness_Id(Long id, Long businessId);

    Optional<EWayBill> findByEwbNumberAndBusiness_Id(String ewbNumber, Long businessId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByBusiness_Id(Long businessId);
}
