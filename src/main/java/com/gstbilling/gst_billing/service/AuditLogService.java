package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.AuditLog;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Business business, String userEmail, String action, String entityType, Long entityId, String details) {
        try {
            AuditLog log = new AuditLog(business, userEmail, action, entityType, entityId, details);
            auditLogRepository.save(log);
        } catch (Exception ignored) {}
    }

    public void logCurrent(String action, String entityType, Long entityId, String details) {
        try {
            var currentUser = currentUserService.getCurrentUser();
            log(currentUser.getBusiness(), currentUser.getEmail(), action, entityType, entityId, details);
        } catch (Exception ignored) {}
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getBusinessLogs() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return auditLogRepository.findByBusiness_IdOrderByCreatedAtDesc(businessId);
    }
}
