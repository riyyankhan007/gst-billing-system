package com.gstbilling.gst_billing.integration.payment;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String orderId,
        BigDecimal amount,
        String currency,
        String provider,
        String checkoutUrl,
        String keyId,
        String status
) {}
