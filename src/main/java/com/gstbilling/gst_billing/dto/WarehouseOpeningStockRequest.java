package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WarehouseOpeningStockRequest(
        @NotNull(message = "Warehouse ID is required") Long warehouseId,
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Quantity is required") BigDecimal quantity,
        String reference,
        String notes
) {}
