package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.dto.DashboardResponse;
import com.gstbilling.gst_billing.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboardMetrics() {
        return dashboardService.getDashboardMetrics();
    }
}
