package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WarehouseTransferRequest(
        @NotNull(message = "Source warehouse ID is required") Long sourceWarehouseId,
        @NotNull(message = "Destination warehouse ID is required") Long destinationWarehouseId,
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Quantity is required") BigDecimal quantity,
        String notes
) {}
