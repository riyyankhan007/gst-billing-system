package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.AuditLog;
import com.gstbilling.gst_billing.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'SUPPORT')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLog> getAuditLogs() {
        return auditLogService.getBusinessLogs();
    }
}
