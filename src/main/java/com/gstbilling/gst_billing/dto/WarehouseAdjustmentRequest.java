package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WarehouseAdjustmentRequest(
        @NotNull(message = "Warehouse ID is required") Long warehouseId,
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "New quantity is required") BigDecimal newQuantity,
        String reason,
        String reference
) {}
