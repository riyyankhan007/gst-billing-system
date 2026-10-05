package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.health.HealthStatus;
import com.gstbilling.gst_billing.health.SystemHealthResponse;
import com.gstbilling.gst_billing.health.SystemHealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final SystemHealthService healthService;

    public HealthController(SystemHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponse> getHealth() {
        SystemHealthResponse health = healthService.getHealth();
        HttpStatus status = (health.status() == HealthStatus.UP) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(healthService.getMetrics());
    }
}
