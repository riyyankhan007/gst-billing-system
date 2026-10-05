package com.gstbilling.gst_billing.health;

import java.util.Map;

public record ComponentHealth(
        HealthStatus status,
        String description,
        Map<String, Object> details
) {}
