package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull(message = "Invoice ID is required")
        Long invoiceId,

        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be greater than zero")
        BigDecimal amount,

        LocalDate paymentDate,

        @NotNull(message = "Payment method is required")
        String paymentMethod,

        String referenceNumber,
        String notes,
        String gatewayProvider,
        String gatewayPaymentId,
        String gatewayOrderId
) {
    public PaymentRequest(Long invoiceId, BigDecimal amount, LocalDate paymentDate, String paymentMethod, String referenceNumber, String notes) {
        this(invoiceId, amount, paymentDate, paymentMethod, referenceNumber, notes, null, null, null);
    }
}
