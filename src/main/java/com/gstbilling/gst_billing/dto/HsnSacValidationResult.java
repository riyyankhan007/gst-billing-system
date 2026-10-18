package com.gstbilling.gst_billing.dto;

public record HsnSacValidationResult(
        boolean valid,
        String code,
        String type,
        boolean existsInCatalogue,
        HsnSacCodeDto item,
        String message
) {}
