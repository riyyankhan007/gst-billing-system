package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.AuditLog;
import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.repository.AuditLogRepository;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.security.CorrelationContext;
import com.gstbilling.gst_billing.security.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final BusinessRepository businessRepository;
    private final CurrentUserService currentUserService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            BusinessRepository businessRepository,
            CurrentUserService currentUserService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.businessRepository = businessRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            Business business,
            Long actorUserId,
            String userEmail,
            String action,
            String entityType,
            Long entityId,
            String details,
            String correlationId,
            String ipAddress,
            String userAgent
    ) {
        try {
            if (business == null && currentUserService.getCurrentTenantId() != null) {
                business = businessRepository.findById(currentUserService.getCurrentTenantId()).orElse(null);
            }
            if (business == null) {
                return;
            }

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = resolveCorrelationId();
            }
            if (ipAddress == null || ipAddress.isBlank()) {
                ipAddress = resolveClientIp();
            }
            if (userAgent == null || userAgent.isBlank()) {
                userAgent = resolveUserAgent();
            }

            AuditLog log = new AuditLog(business, actorUserId, userEmail, action, entityType, entityId, details, correlationId, ipAddress, userAgent);
            auditLogRepository.save(log);
        } catch (Exception ignored) {}
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Business business, String userEmail, String action, String entityType, Long entityId, String details) {
        log(business, null, userEmail, action, entityType, entityId, details, null, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, Long entityId, String details) {
        try {
            var currentUser = currentUserService.getCurrentUser();
            Long businessId = currentUserService.getCurrentTenantId();
            Business business = (businessId != null)
                    ? businessRepository.findById(businessId).orElseGet(currentUser::getBusiness)
                    : currentUser.getBusiness();

            log(
                    business,
                    currentUser.getId(),
                    currentUser.getEmail(),
                    action,
                    entityType,
                    entityId,
                    details,
                    resolveCorrelationId(),
                    resolveClientIp(),
                    resolveUserAgent()
            );
        } catch (Exception ignored) {}
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getBusinessLogs() {
        Long businessId = currentUserService.getCurrentTenantId();
        if (businessId == null) {
            businessId = currentUserService.getCurrentUser().getBusiness().getId();
        }
        return auditLogRepository.findByBusiness_IdOrderByCreatedAtDesc(businessId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getBusinessLogsByEntityType(String entityType) {
        Long businessId = currentUserService.getCurrentTenantId();
        if (businessId == null) {
            businessId = currentUserService.getCurrentUser().getBusiness().getId();
        }
        return auditLogRepository.findByBusiness_IdAndEntityTypeOrderByCreatedAtDesc(businessId, entityType);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByEntityType(Long businessId, String entityType) {
        if (businessId == null) {
            businessId = currentUserService.getCurrentTenantId();
        }
        return auditLogRepository.findByBusiness_IdAndEntityTypeOrderByCreatedAtDesc(businessId, entityType);
    }

    private String resolveCorrelationId() {
        String id = CorrelationContext.getCorrelationId();
        if (id != null && !id.isBlank()) {
            return id;
        }
        id = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (id != null && !id.isBlank()) {
            return id;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xfHeader = req.getHeader("X-Forwarded-For");
                if (xfHeader != null && !xfHeader.isBlank()) {
                    return xfHeader.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String resolveUserAgent() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ua = req.getHeader("User-Agent");
                if (ua != null && !ua.isBlank()) {
                    return ua.length() > 255 ? ua.substring(0, 255) : ua;
                }
            }
        } catch (Exception ignored) {}
        return "INTERNAL";
    }
}
