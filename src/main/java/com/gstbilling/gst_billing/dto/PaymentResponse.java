package com.gstbilling.gst_billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        Long customerId,
        String customerName,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        String referenceNumber,
        String notes,
        String createdBy,
        LocalDateTime createdAt,
        BigDecimal invoiceGrandTotal,
        BigDecimal invoicePaidAmount,
        BigDecimal invoiceBalanceAmount,
        String invoiceStatus
) {}
