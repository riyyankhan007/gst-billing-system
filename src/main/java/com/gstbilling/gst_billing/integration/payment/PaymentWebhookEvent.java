package com.gstbilling.gst_billing.integration.payment;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentWebhookEvent(
        String eventType,
        String orderId,
        String paymentId,
        BigDecimal amount,
        String currency,
        String status,
        String signature,
        Map<String, String> notes
) {}
