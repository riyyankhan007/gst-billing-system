package com.gstbilling.gst_billing.integration.payment;

import java.math.BigDecimal;
import java.util.Map;

public record PaymentOrderRequest(
        Long invoiceId,
        BigDecimal amount,
        String currency,
        String customerName,
        String customerEmail,
        String customerPhone,
        String receipt,
        Map<String, String> notes
) {}
