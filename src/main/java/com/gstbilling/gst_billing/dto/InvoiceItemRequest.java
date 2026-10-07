package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        String productName,

        String hsnCode,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "GST rate cannot be negative")
        BigDecimal gstRate,

        @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be negative")
        BigDecimal discount,

        Boolean taxInclusive
) {
    public boolean isTaxInclusive() {
        return Boolean.TRUE.equals(taxInclusive);
    }
}
