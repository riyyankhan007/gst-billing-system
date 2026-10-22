package com.gstbilling.gst_billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBusinessRequest(
        @NotBlank(message = "Business name is required") String name,
        String gstin,
        String pan,
        String address,
        String state,
        String stateCode,
        String phone,
        String email,
        String invoicePrefix
) {}
