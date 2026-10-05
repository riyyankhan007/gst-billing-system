package com.gstbilling.gst_billing.health;

import java.time.LocalDateTime;
import java.util.Map;

public record SystemHealthResponse(
        HealthStatus status,
        LocalDateTime timestamp,
        long uptimeSeconds,
        Map<String, ComponentHealth> components
) {}
