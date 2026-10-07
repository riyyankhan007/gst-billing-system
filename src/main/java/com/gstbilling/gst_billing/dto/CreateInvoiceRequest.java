package com.gstbilling.gst_billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        LocalDate invoiceDate,

        LocalDate dueDate,

        String invoiceNumber,

        @DecimalMin(value = "0.0", inclusive = true, message = "Discount amount cannot be negative")
        BigDecimal discountAmount,

        Boolean reverseCharge,

        Boolean exportType,

        String notes,

        String termsAndConditions,

        String status,

        @NotEmpty(message = "Invoice must contain at least one line item")
        @Valid
        List<InvoiceItemRequest> items
) {}
