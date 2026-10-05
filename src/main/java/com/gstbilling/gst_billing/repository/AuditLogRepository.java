package com.gstbilling.gst_billing.repository;

import com.gstbilling.gst_billing.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByBusiness_IdOrderByCreatedAtDesc(Long businessId);
    List<AuditLog> findByBusiness_IdAndEntityTypeOrderByCreatedAtDesc(Long businessId, String entityType);
    List<AuditLog> findByBusiness_IdAndCorrelationId(Long businessId, String correlationId);
    @org.springframework.transaction.annotation.Transactional
    void deleteByBusiness_Id(Long businessId);
}
